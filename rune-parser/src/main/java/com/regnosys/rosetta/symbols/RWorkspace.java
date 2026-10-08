package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationPass;
import java.nio.file.Path;

import com.regnosys.rosetta.symbols.index.DependencyIndex;
import com.regnosys.rosetta.symbols.index.NameSearchIndex;
import com.regnosys.rosetta.symbols.index.ReferenceIndex;
import com.regnosys.rosetta.symbols.index.SubTypeIndex;
import com.regnosys.rosetta.symbols.linker.DerivedStatePass;
import com.regnosys.rosetta.symbols.linker.GlobalResolutionPass;
import com.regnosys.rosetta.symbols.linker.ImportResolutionPass;
import com.regnosys.rosetta.symbols.linker.LexicalResolutionPass;
import com.regnosys.rosetta.symbols.linker.SymbolRegistrationPass;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.inference.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The immutable result of building a set of files through the M3 linker.
 * Holds the symbol table, all file scopes, the traceability indexes,
 * and the diagnostic list.
 *
 * <p>Single entry point: {@link #build(List)}. After construction the
 * workspace container does not support structural mutation through its
 * exposed collections, and linker-driven AST resolution is complete before
 * {@code build} returns. However, callers should not assume deep immutability
 * of objects returned by this API (for example, namespace scope instances
 * retain their registration methods for cross-package access).
 *
 * <p><b>Identity equality:</b> {@code equals} / {@code hashCode} /
 * {@code toString} are not overridden. Each {@link #build(List)} call returns
 * a fresh instance never aliased elsewhere, so identity-based equality is
 * correct and matches the H7/U005 generation semantics (two workspace
 * instances always have distinct generation tokens). Records-everywhere
 * migration in P2 may revisit.
 *
 * <p>Spec: D4 + D8 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class RWorkspace implements SymbolResolver {

    /**
     * Process-wide monotonic counter for workspace generation tokens.
     * Each {@link #build(List)} invocation increments and reads this counter
     * to seed its workspace's {@link #generation()}.
     *
     * <p>Per H7 / U005. Ensures SymbolIds issued by one workspace cannot
     * silently resolve against a re-built workspace.
     */
    private static final AtomicLong GENERATION_SEQ = new AtomicLong();

    private final long generation;
    private final List<RModel> files;
    private final Map<String, RNamespaceScope> namespaces;
    private final Map<RModel, RFileScope> fileScopes;
    private final DependencyIndex dependencyIndex;
    private final SubTypeIndex subTypeIndex;
    private final ReferenceIndex referenceIndex;
    private final NameSearchIndex nameSearchIndex;
    private final List<LinkingDiagnostic> linkingDiagnostics;
    private final List<ValidationDiagnostic> validationDiagnostics;
    private final TypeInferenceEngine typeEngine;
    private final CardinalityComputer cardinalityComputer;
    private final CellPreference cellPreference;
    private final int headShadowingConflicts;

    RWorkspace(
            long generation,
            List<RModel> files,
            Map<String, RNamespaceScope> namespaces,
            Map<RModel, RFileScope> fileScopes,
            DependencyIndex dependencyIndex,
            SubTypeIndex subTypeIndex,
            ReferenceIndex referenceIndex,
            NameSearchIndex nameSearchIndex,
            List<LinkingDiagnostic> linkingDiagnostics,
            List<ValidationDiagnostic> validationDiagnostics,
            TypeInferenceEngine typeEngine,
            CardinalityComputer cardinalityComputer,
            CellPreference cellPreference,
            int headShadowingConflicts) {
        this.headShadowingConflicts = headShadowingConflicts;
        this.generation = generation;
        this.files = List.copyOf(files);
        this.namespaces = Collections.unmodifiableMap(new TreeMap<>(namespaces));
        this.fileScopes = new IdentityHashMap<>(fileScopes);
        this.dependencyIndex = dependencyIndex;
        this.subTypeIndex = subTypeIndex;
        this.referenceIndex = referenceIndex;
        this.nameSearchIndex = nameSearchIndex;
        this.linkingDiagnostics = List.copyOf(linkingDiagnostics);
        this.validationDiagnostics = List.copyOf(validationDiagnostics);
        this.typeEngine = typeEngine;
        this.cardinalityComputer = cardinalityComputer;
        this.cellPreference = cellPreference;
    }

    /**
     * Builds a workspace from the given files. Runs all six linker passes:
     * symbol registration, import resolution, derived state, global
     * resolution, lexical resolution, and type inference.
     *
     * @param files non-null list of non-null {@link RModel} files to link
     * @throws NullPointerException if {@code files} is null or any element is null
     */
    public static RLinkingResult build(List<RModel> files) {
        return build(files, List.of());
    }

    /**
     * PR #445 — builds a workspace with CELL provenance: each root in
     * {@code cellRoots} names one upstream build closure's load root (a
     * "cell"); models are assigned to cells by longest path prefix and name
     * resolution prefers same-file, then same-cell candidates on duplicate
     * FQNs (see {@link CellPreference}). Pass the finest closure roots — for
     * the merged cdm corpus that is each cdm cell's own directory, not their
     * shared parent. An empty list = the plain {@link #build(List)} behaviour
     * (same-file preference only, which is a no-op wherever namespaces carry
     * no duplicate names — every single-closure population).
     *
     * @param files non-null list of non-null {@link RModel} files to link
     * @param cellRoots cell load roots for provenance; may be empty
     * @throws NullPointerException if {@code files} is null or any element is null
     */
    public static RLinkingResult build(List<RModel> files, List<Path> cellRoots) {
        Objects.requireNonNull(files, "files must not be null");
        for (int i = 0; i < files.size(); i++) {
            Objects.requireNonNull(files.get(i),
                    "files[" + i + "] must not be null");
        }
        // S1: validate input BEFORE incrementing the seq, so a null-arg call
        // doesn't burn a generation token (would leave a visible gap in build
        // logs / test output).
        long generation = GENERATION_SEQ.incrementAndGet();
        Diagnostics collector = new Diagnostics();
        DependencyIndex dependencyIndex = new DependencyIndex();
        SubTypeIndex subTypeIndex = new SubTypeIndex();
        ReferenceIndex referenceIndex = new ReferenceIndex();
        // PR #445: cell provenance for the same-file/same-cell resolution
        // preference (no-op for empty cellRoots / single-closure populations).
        CellPreference cellPreference = CellPreference.fromRoots(files, cellRoots);

        // Pass 1: register all top-level declarations
        Map<String, RNamespaceScope> namespaces =
            new SymbolRegistrationPass().run(files, collector);

        // Pass 2: build per-file visible scopes + dependency index
        Map<RModel, RFileScope> fileScopes =
            new ImportResolutionPass().run(files, namespaces, dependencyIndex, collector,
                cellPreference);

        // Pass 3: derived state (purely structural, before resolution)
        new DerivedStatePass().run(files, collector);

        // Pass 4: global cross-ref resolution (T8 types + T9 rest)
        new GlobalResolutionPass(generation, namespaces, fileScopes, subTypeIndex, referenceIndex,
                cellPreference)
            .run(files, collector);

        // P1.4.1b / H7: attach every reachable node to a lightweight namespace-based
        // resolver RIGHT AFTER pass 4, so that superType() / superFunction() / etc.
        // lazy-resolve accessors are usable by passes 5-7 (type inference + validation).
        // After the full workspace is constructed, nodes are re-attached to the workspace
        // instance (which also implements SymbolResolver) — the WeakReference is simply
        // overwritten with the stronger workspace reference.
        //
        // The interim resolver is private to this method; it is replaced by the workspace
        // itself after workspace construction, so no dangling reference can leak out.
        // This sequencing is guarded by Task 5's BindFreezeResolveLifecycleTest.
        final Map<String, RNamespaceScope> nsSnapshot = namespaces;
        final Map<RModel, RFileScope> fsSnapshot = fileScopes;
        final long generationSnapshot = generation;
        final CellPreference prefsSnapshot = cellPreference;
        SymbolResolver interimResolver = new SymbolResolver() {
            @Override public long generation() { return generationSnapshot; }
            @Override
            public <T extends RNode> T resolve(SymbolId id, Class<T> expected) {
                return resolve(id, expected, null);
            }
            @Override
            public <T extends RNode> T resolve(SymbolId id, Class<T> expected, RNode requester) {
                Objects.requireNonNull(expected, "expected must not be null");
                if (id == null) return null;
                if (id.generation() != generationSnapshot) {
                    throw new StaleSymbolIdException(id, generationSnapshot);
                }
                Optional<RRootElement> target;
                if (SymbolResolver.BUILTIN_NAMESPACE.equals(id.namespace())) {
                    target = fsSnapshot.values().stream()
                            .map(RFileScope::builtinNamespace)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .flatMap(bn -> bn.lookup(id.localName()));
                } else {
                    RNamespaceScope ns = nsSnapshot.get(id.namespace());
                    if (ns == null) return null;
                    // PR #445: the requester-ranked pick (same file, same cell,
                    // registration order) — identical to ns.lookup when the
                    // name has no duplicates or no requester is known.
                    target = prefsSnapshot.pick(ns.allMatching(id.localName()),
                            requester == null ? null : CellPreference.modelOf(requester));
                }
                if (target.isEmpty()) return null;
                RRootElement found = target.get();
                if (!expected.isInstance(found)) {
                    throw new IllegalArgumentException(
                            "SymbolId " + id.fqn() + " resolves to "
                                    + found.getClass().getSimpleName()
                                    + ", but caller expected " + expected.getSimpleName());
                }
                return expected.cast(found);
            }
            @Override
            public RNode resolveTypeLike(SymbolId id) {
                return resolveTypeLikeIn(id, generationSnapshot, nsSnapshot, fsSnapshot.values(),
                        prefsSnapshot, null);
            }
            @Override
            public RNode resolveTypeLike(SymbolId id, RNode requester) {
                return resolveTypeLikeIn(id, generationSnapshot, nsSnapshot, fsSnapshot.values(),
                        prefsSnapshot, requester);
            }
        };
        for (RModel file : files) {
            com.regnosys.rosetta.ast.util.AstWalker.walk(file, n -> n.attachToWorkspace(interimResolver));
        }

        // Pass 5: lexical cross-ref resolution (T10)
        LexicalResolutionPass lexicalPass = new LexicalResolutionPass(fileScopes, referenceIndex);
        lexicalPass.run(files, collector);
        // v3.1 C1 MF-4 — the pass counts the ONE ordering question C1 chose not
        // to answer by fiat (see LexicalResolutionPass.headShadowingConflicts).
        // Until this line the count had no reader anywhere in the tree: an
        // unwired meter, which reads exactly like a meter reading zero. The
        // count rides the workspace so CorpusDiagnosticGateTest can assert it
        // per cell.
        int headShadowingConflicts = lexicalPass.headShadowingConflicts();

        // Pass 6: type inference + type-directed resolution (M4)
        BuiltinTypeRegistry builtinRegistry = BuiltinTypeRegistry.createDefault();
        SubtypeRelation subtypeRelation = new SubtypeRelation();
        TypeJoin typeJoin = new TypeJoin(subtypeRelation);
        TypeAliasSolver aliasSolver = new TypeAliasSolver();
        ExpressionTypeComputer typeComputer = new ExpressionTypeComputer(
            builtinRegistry, subtypeRelation, typeJoin, aliasSolver);
        TypeDirectedResolver typeResolver = new TypeDirectedResolver(builtinRegistry);
        TypeInferenceEngine typeEngine = new TypeInferenceEngine(typeComputer);
        typeEngine.setResolver(typeResolver);
        // Wire the shared ReferenceIndex so Cat 9 + Cat 10 success paths can
        // register their cross-references — mirrors passes 4 + 5 which pair
        // every setResolvedSymbol with referenceIndex.registerReference. Without
        // this, findReferences() misses the symbol references stamped in pass 6.
        // Copilot PR #76 R11 F1.
        typeEngine.setReferenceIndex(referenceIndex);
        // v3.1 C1 (spec R10) — the meta-type channel. Upstream resolves a meta
        // description against EXPORTED metaType root elements visible from the
        // reference's own resource set, so the lookup is per-FILE: find the
        // RModel containing the node, then ask that file's scope, kind-filtered
        // to RMetaType. Kind-filtering matters for the same reason it does at
        // every other seat — upstream's scoping is ECLASS-typed, so a same-named
        // non-metaType declaration must not shadow the metaType.
        typeEngine.setMetaTypeLookup((context, name) -> {
            RModel owner = com.regnosys.rosetta.ast.util.AstWalker
                    .findAncestor(context, RModel.class).orElse(null);
            if (owner == null) {
                return java.util.Optional.empty();
            }
            RFileScope scope = fileScopes.get(owner);
            if (scope == null) {
                return java.util.Optional.empty();
            }
            return scope.lookupOfKind(name,
                            com.regnosys.rosetta.ast.types.RMetaType.class::isInstance)
                    .map(com.regnosys.rosetta.ast.types.RMetaType.class::cast);
        });
        typeEngine.run(files, collector);

        CardinalityComputer cardinalityComputer = new CardinalityComputer();

        // Pass 7: Semantic validation (M6)
        ValidationCollector validationCollector = new ValidationCollector();
        ValidationPass validationPass = new ValidationPass(List.of(
            new com.regnosys.rosetta.validation.validators.NamingValidator(),
            new com.regnosys.rosetta.validation.validators.UniquenessValidator(),
            new com.regnosys.rosetta.validation.validators.EnumValidator(),
            new com.regnosys.rosetta.validation.validators.ChoiceValidator(),
            // PR #455: the unused-import check's string-ref approximation
            // consults the symbol table (see the ImportValidator field note).
            new com.regnosys.rosetta.validation.validators.ImportValidator(namespaces),
            new com.regnosys.rosetta.validation.validators.TypeValidator(),
            // PR #442: AttributeValidator gained the type channel (upstream's
            // subtype-based override check); the fork-native ConditionValidator
            // (duplicate condition names) was RETIRED — upstream has no such
            // check anywhere (neither ConditionValidator.checkConditionName nor
            // any RosettaUniqueNamesConfig duplication cluster covers Condition),
            // and shipped DRR models legally carry duplicate condition names.
            new com.regnosys.rosetta.validation.validators.AttributeValidator(typeEngine, subtypeRelation),
            new com.regnosys.rosetta.validation.validators.FunctionValidator(),
            new com.regnosys.rosetta.validation.validators.ExpressionValidator(typeEngine, subtypeRelation, cardinalityComputer, typeJoin, typeResolver),
            new com.regnosys.rosetta.validation.validators.MetadataValidator(),
            new com.regnosys.rosetta.validation.validators.SynonymValidator(),
            // PR #455: the rule-reference cardinality arm reads rule output
            // cardinality through the faithful computeRuleBody entry.
            new com.regnosys.rosetta.validation.validators.ReportValidator(cardinalityComputer),
            new com.regnosys.rosetta.validation.validators.ConstructorValidator(),
            new com.regnosys.rosetta.validation.validators.MiscValidator()
        ), typeResolver);
        validationPass.run(files, validationCollector);

        // Name search index (T12) — built BEFORE workspace ctor (which takes it as arg).
        NameSearchIndex nameSearchIndex = NameSearchIndex.build(namespaces);

        // Construct workspace BEFORE freeze. Single instance — no double-construction.
        RWorkspace ws = new RWorkspace(
            generation,
            files, namespaces, fileScopes, dependencyIndex,
            subTypeIndex, referenceIndex, nameSearchIndex, collector.toList(),
            validationCollector.toList(), typeEngine, cardinalityComputer,
            cellPreference, headShadowingConflicts);

        // P1.4.1b / H7 / U005: keep the interimResolver strongly reachable through passes
        // 5-7 so that superType() / other lazy-resolve accessors can dereference the
        // WeakReference. Without this fence the JIT may determine that the local variable
        // `interimResolver` is dead after line 192 (its last explicit mention), making the
        // WeakReference collectible before passes 5-7 finish on large corpora (CDM 5.x).
        // Reference.reachabilityFence() is the JDK9+ standard idiom for exactly this
        // pattern (see java.lang.ref.Reference javadoc and JEP 193).
        java.lang.ref.Reference.reachabilityFence(interimResolver);

        // Re-attach every reachable node to the FULL workspace (overwriting the interim
        // resolver WeakReference set after pass 4). After this point, workspace().resolve(...)
        // routes through the workspace's own SymbolId lookup, backed by the frozen + immutable
        // namespace map.
        // Order: pass 7 → reachabilityFence → re-attach to workspace → freeze. Do NOT reorder —
        // this ordering is guarded by BindFreezeResolveLifecycleTest.
        for (RModel file : files) {
            com.regnosys.rosetta.ast.util.AstWalker.walk(file, n -> n.attachToWorkspace(ws));
        }

        // P1.4.1a / D12: freeze every reachable RNode after pass-7 validation
        // completes. Subsequent setter calls on a frozen node throw
        // IllegalStateException. Per docs/upgrades/U002-frozen-ast-after-linker.md.
        for (RModel file : files) {
            file.freeze();
        }

        return new RLinkingResult(ws);
    }

    /**
     * Returns this workspace's generation token. Stable for the lifetime of the
     * workspace; strictly increasing across {@link #build(List)} invocations.
     *
     * <p>Per H7 / U005.
     */
    @Override
    public long generation() {
        return generation;
    }

    /**
     * Resolves a {@link SymbolId} to its target node.
     *
     * <p>Behaviour (per {@link SymbolResolver} contract — expected-null check FIRST):
     * <ul>
     *   <li>{@code expected == null} → {@link NullPointerException}.</li>
     *   <li>{@code id == null} → returns {@code null}.</li>
     *   <li>{@code id.generation() != this.generation()} →
     *       {@link StaleSymbolIdException} with caller-friendly message.</li>
     *   <li>Unknown namespace OR unknown local name → returns {@code null}
     *       (signals a dangling reference; callers decide whether that is
     *       diagnostic-worthy).</li>
     *   <li>Resolved target's runtime class is not assignable to
     *       {@code expected} → {@link IllegalArgumentException} with
     *       caller-friendly message.</li>
     *   <li>Otherwise → resolved target cast to {@code T}.</li>
     * </ul>
     *
     * <p>Resolution is via direct namespace lookup; cost is one
     * {@link Map#get} on {@code namespaces} plus one
     * {@link RNamespaceScope#lookup(String)} call. Per-call HashMap lookup is
     * accepted for P1.4.1b; D5 wall-clock budget gate catches regressions.
     * Memoization (transient cache field on cross-ref-bearing nodes) is the
     * documented escalation path per spec §12 risk #2.
     *
     * <p>SF8 — builtin namespace routing. Builtins ({@code string}, {@code int},
     * {@code RDate}, etc.) are not in the user-facing {@code namespaces} map;
     * they live in each file's {@code builtinNamespace}. All RFileScopes share
     * the same {@code RNamespaceScope} reference for the {@code com.rosetta.model}
     * namespace, retrieved once in {@link
     * com.regnosys.rosetta.symbols.linker.ImportResolutionPass#run} and passed
     * to every {@code RFileScope}. So the first non-null builtin ns from any
     * file scope is the canonical one. {@code RFileScope.builtinNamespace()}
     * is {@code null} when no input file declares the {@code com.rosetta.model}
     * namespace (typical for unit-test workspaces) — those are filtered out.
     * Empty fileScopes (zero-file workspace) → returns {@code null} for builtin
     * targets, which is the correct behaviour for "no builtins available".
     *
     * <p>Cost note: builtin-namespace resolution scans up to N file scopes to
     * find the first non-null builtinNamespace (acceptable because file count
     * is bounded by corpus size; see spec §12 risk #2 for the documented
     * escalation path if the per-call cost becomes a hot path).
     *
     * @param id the symbol id, or {@code null}
     * @param expected the expected runtime class of the target; must not be {@code null}
     * @param <T> the expected target type
     * @return the resolved target, or {@code null} if {@code id} is null or unknown
     * @throws NullPointerException if {@code expected} is null
     * @throws StaleSymbolIdException if {@code id.generation()} does not match
     *     this workspace's generation
     * @throws IllegalArgumentException if the resolved target is not assignable
     *     to {@code expected}
     */
    @Override
    public <T extends RNode> T resolve(SymbolId id, Class<T> expected) {
        return resolve(id, expected, null);
    }

    /**
     * PR #445 — the requester-aware resolve (see
     * {@link SymbolResolver#resolve(SymbolId, Class, RNode)}): candidate
     * picked by the {@link CellPreference} rank (same file, same cell,
     * registration order) relative to the resolving node's declaring model.
     * Identical to the requester-less form when the local name has no
     * duplicate declarations or no requester is supplied.
     */
    @Override
    public <T extends RNode> T resolve(SymbolId id, Class<T> expected, RNode requester) {
        Objects.requireNonNull(expected, "expected must not be null");
        if (id == null) return null;
        if (id.generation() != this.generation) {
            throw new StaleSymbolIdException(id, this.generation);
        }

        Optional<RRootElement> target;
        if (SymbolResolver.BUILTIN_NAMESPACE.equals(id.namespace())) {
            // SF8: scan file scopes for the shared builtinNamespace; null-filter for unit-test workspaces.
            target = fileScopes.values().stream()
                    .map(RFileScope::builtinNamespace)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .flatMap(bn -> bn.lookup(id.localName()));
        } else {
            RNamespaceScope ns = namespaces.get(id.namespace());
            if (ns == null) return null;
            target = cellPreference.pick(ns.allMatching(id.localName()),
                    requester == null ? null : CellPreference.modelOf(requester));
        }

        if (target.isEmpty()) return null;
        RRootElement found = target.get();
        if (!expected.isInstance(found)) {
            throw new IllegalArgumentException(
                    "SymbolId " + id.fqn() + " resolves to "
                            + found.getClass().getSimpleName()
                            + ", but caller expected " + expected.getSimpleName());
        }
        return expected.cast(found);
    }

    /**
     * Resolves a {@link SymbolId} known to refer to a type-like declaration
     * (one of {@link RDataType}, {@link REnumeration}, {@link RChoice},
     * {@link RBasicType}, {@link RRecordType}, {@link RTypeAlias}).
     *
     * <p>Unlike {@link #resolve(SymbolId, Class)} — which uses
     * {@code ns.lookup(localName)} and so returns the FIRST-registered element
     * sharing that local name — this method walks {@code ns.allMatching(localName)}
     * and returns the first match that is type-like. This matters when a rule
     * (or function/shortcut/etc.) and a type share a local name within the
     * same namespace: the rule may be registered first, but a type-position
     * SymbolId (built by {@link com.regnosys.rosetta.symbols.linker.GlobalResolutionPass#resolveTypeCall})
     * must always re-resolve to the type, not the rule.
     *
     * <p>Concrete example: in namespace {@code drr.enrichment.common},
     * both {@code reporting rule EnrichmentData} and {@code type EnrichmentData}
     * are declared; the attribute reference {@code enrichment EnrichmentData}
     * on {@code ReportableInformationBase} must reach the type even when
     * {@code lookup} returns the rule first.
     *
     * @return the resolved type-like target, or {@code null} if {@code id} is
     *     null, the namespace is unknown, or no type-like match exists
     */
    public RNode resolveTypeLike(SymbolId id) {
        return resolveTypeLikeIn(id, this.generation, this.namespaces, this.fileScopes.values(),
                cellPreference, null);
    }

    /**
     * PR #445 — the requester-aware type-position resolve (see
     * {@link SymbolResolver#resolveTypeLike(SymbolId, RNode)}): the
     * type-like walk runs over the {@link CellPreference}-ordered candidate
     * list, so a duplicate FQN resolves same-file/same-cell first.
     */
    @Override
    public RNode resolveTypeLike(SymbolId id, RNode requester) {
        return resolveTypeLikeIn(id, this.generation, this.namespaces, this.fileScopes.values(),
                cellPreference, requester);
    }

    /**
     * Shared implementation of the type-position {@code allMatching}-walk used by
     * the interim resolver attached in pass 4 and the final {@link RWorkspace}
     * instance. Both pass the same logic; this helper eliminates the duplication
     * called out by indep review I2 (CLAUDE.md SDLC rule 3 — single source of
     * truth for drift-prone facts).
     *
     * <p>See the public {@link #resolveTypeLike(SymbolId)} javadoc for the
     * resolution contract; the type-like predicate lives at
     * {@link SymbolResolver#isTypeLike}.
     */
    private static RNode resolveTypeLikeIn(SymbolId id, long expectedGen,
            Map<String, RNamespaceScope> nss, Collection<RFileScope> fs,
            CellPreference prefs, RNode requester) {
        if (id == null) return null;
        if (id.generation() != expectedGen) {
            throw new StaleSymbolIdException(id, expectedGen);
        }
        List<RRootElement> candidates;
        if (SymbolResolver.BUILTIN_NAMESPACE.equals(id.namespace())) {
            candidates = fs.stream()
                    .map(RFileScope::builtinNamespace)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(bn -> bn.allMatching(id.localName()))
                    .orElse(List.of());
        } else {
            RNamespaceScope ns = nss.get(id.namespace());
            if (ns == null) return null;
            candidates = ns.allMatching(id.localName());
        }
        // PR #445: rank the candidates for the requester (same file, same
        // cell, registration order) before the type-like walk — the walk's
        // own kind filter is unchanged.
        for (RRootElement c : prefs.order(candidates,
                requester == null ? null : CellPreference.modelOf(requester))) {
            if (SymbolResolver.isTypeLike(c)) return (RNode) c;
        }
        return null;
    }

    public List<RModel> files() { return files; }

    public Optional<RNamespaceScope> namespace(String qualifiedName) {
        return Optional.ofNullable(namespaces.get(qualifiedName));
    }

    public Map<String, RNamespaceScope> namespacesMap() { return namespaces; }

    public List<RNamespaceScope> namespaces() {
        return List.copyOf(namespaces.values());
    }

    public Optional<RFileScope> fileScope(RModel file) {
        return Optional.ofNullable(fileScopes.get(file));
    }

    // === Traceability APIs (D6) =============================================

    /** Forward: "what does this file import from?" */
    public List<RModel> getDependencies(RModel file) {
        return dependencyIndex.getDependencies(file);
    }

    /** Reverse: "who imports from this file?" */
    public List<RModel> getDependents(RModel file) {
        return dependencyIndex.getDependents(file);
    }

    /** Direct subtypes of a data type. */
    public List<RDataType> getSubTypes(RDataType type) {
        return subTypeIndex.getSubTypes(type);
    }

    /** Full super-type chain (bounded iteration for cycle safety). */
    public List<RDataType> getTypeHierarchy(RDataType type) {
        List<RDataType> chain = new ArrayList<>();
        Optional<RDataType> current = type.superType();
        int safety = 0;
        while (current.isPresent() && safety++ < 1000) {
            chain.add(current.get());
            current = current.get().superType();
        }
        return List.copyOf(chain);
    }

    /** All nodes that reference the given target. */
    public List<RNode> findReferences(RNode target) {
        return referenceIndex.findReferences(target);
    }

    /** Exact + fuzzy name search across all qualified names (D6). */
    public List<RNode> findByName(String query) {
        return nameSearchIndex.findByName(query);
    }

    // === M4 Type System APIs =================================================

    /** Returns the inferred type of an expression (M4). */
    public RMetaAnnotatedType getInferredType(RExpression expr) {
        return typeEngine.getInferredType(expr);
    }

    /**
     * Resolves an attribute's declared {@code typeCall} to its {@link RMetaAnnotatedType}
     * (MISSING when unresolvable) — the ATTRIBUTE-channel twin of {@link #getInferredType},
     * which is an expression-node-keyed cache and so cannot type a node the fixed point never
     * saw. Exposed for the IR adapter's #478 disguised-input-nav arm: its synthesized
     * equivalent receiver is a fresh node, so the receiver's type comes from the resolved head
     * attribute through this computed channel (the same {@code inferTypeOfAttribute} resolution
     * the #442/#454 validator seats read on the engine).
     */
    public RMetaAnnotatedType getInferredAttributeType(
            com.regnosys.rosetta.ast.supporting.RAttribute attr) {
        return typeEngine.getInferredAttributeType(attr);
    }

    /** Returns the cardinality (SINGLE/MULTI) of an expression (M4). */
    public ExpressionCardinality getCardinality(RExpression expr) {
        return cardinalityComputer.compute(expr);
    }

    /**
     * Returns the cardinality of a reporting-rule BODY expression — like
     * {@link #getCardinality(RExpression)} but treats a {@code then} pipe
     * faithfully (the result follows the argument's cardinality through the
     * inline-function body) instead of the conservative {@code SINGLE} the
     * global path returns. Consumed by the rule-emission path (RuleGenerator's
     * synthetic-output cardinality back-fill + the rule whole-output SET
     * terminal coercion) so the function tail's {@code then}-rendering stays
     * byte-frozen, and — since the #492 alias-nav teach — by the IR adapter's
     * alias-BODY cardinality read (a shortcut body is overwhelmingly a disguised
     * {@code REnumValueRef} chain, which the global path reads as conservative
     * SINGLE; an under-read MULTI would let a multi alias chain pass the scalar
     * operand gates) — and by the default route's function-path deep-then hoist
     * ({@code CollectionHandler}: the conditional consumer's multi verdict and, since
     * v3.2 seat 2, the literal-switch consumer's SINGLE-pipe admission) — admission reads
     * that only decline a restructure, never rendering reads. See
     * {@code CardinalityComputer.computeRuleBody}.
     */
    public ExpressionCardinality getRuleBodyCardinality(RExpression expr) {
        return cardinalityComputer.computeRuleBody(expr);
    }

    /** Number of fixed-point iterations the type engine took. */
    public int typeInferenceIterations() {
        return typeEngine.iterationCount();
    }

    /** All diagnostics (linking + validation). M8 LSP consumes this. */
    public List<RDiagnostic> diagnostics() {
        List<RDiagnostic> all = new ArrayList<>();
        all.addAll(linkingDiagnostics);
        all.addAll(validationDiagnostics);
        return List.copyOf(all);
    }

    /** M3/M4 linking diagnostics only. */
    public List<LinkingDiagnostic> linkingDiagnostics() { return linkingDiagnostics; }

    /** M6 validation diagnostics only. */
    public List<ValidationDiagnostic> validationDiagnostics() { return validationDiagnostics; }

    /**
     * v3.1 C1 (MF-4) — how many {@code a -> b} heads pass 4 bound to a global
     * enumeration while the lexical chain offered a DIFFERENT binding.
     *
     * <p>Upstream's parent chain puts the file scope LAST, so a lexical binding
     * should shadow a same-named global enumeration; the fork's phase order
     * resolves globals FIRST. C1 chose to COUNT that disagreement rather than
     * flip it, because flipping is a behaviour change on cells that are
     * byte-EXACT today. Expected ZERO across the 25-cell band — and "expected"
     * is only worth saying once something reads the number, which is what this
     * accessor and {@code CorpusDiagnosticGateTest} are for. A non-zero value
     * is the flip decision arriving with evidence, not noise.
     */
    public int headShadowingConflicts() {
        return headShadowingConflicts;
    }

    /** True when there are no ERROR-severity diagnostics (warnings OK). */
    public boolean isFullyResolved() {
        return diagnostics().stream().noneMatch(d -> d.severity() == Severity.ERROR);
    }
}
