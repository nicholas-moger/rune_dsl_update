package com.regnosys.rosetta.generator.java.function;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.lang.model.SourceVersion;

/**
 * The function-body naming session for hoisted statement locals — the
 * {@code ifThenElseResult} group (facet {@code ifthenelse_result_hoisting}),
 * the {@code bigInteger} group (facet {@code biginteger_literal_hoist}) and
 * the {@code boolean} group (facet {@code boolean_condition_hoist}).
 *
 * <p>Upstream resolves a hoisted local's name through ONE
 * {@code assignOutputBodyScope} spanning every operation of the function
 * (FunctionGenerator.xtend method scope + {@code .bodyScope}), so the
 * {@code computeActualNames} group law applies across statements: a singleton
 * group keeps the bare base name; a group of n &ge; 2 numbers every member
 * {@code base0..n-1} in registration order (a bare name never coexists with
 * numbered ones). Groups number INDEPENDENTLY of each other — the jfsa
 * GetNtnlQty golden interleaves {@code bigInteger0/1} with
 * {@code ifThenElseResult0/1}. The fork renders operations eagerly through
 * per-statement scopes, so a group's size is unknowable until the LAST
 * operation has rendered — this session is the function-level
 * deferred-naming bridge: registrations hand out opaque sentinels embedded in
 * the rendered statements, and {@link #resolve(String)} substitutes the
 * group-law names once {@code FunctionGenerator.compileOperations} has
 * rendered every operation. The count-based numbering mirrors
 * {@code renderThenExtractSet}'s {@code thenArg}/{@code thenArgN} naming
 * (PR #98), lifted from statement scope to function scope.
 *
 * <p>The sentinel counter is global (not per-session) for the same reason as
 * {@code JavaStatementScope}'s coercion-param counter: two sessions' rendered
 * strings must never carry the same token, or the first resolution's literal
 * {@link String#replace} would silently consume both occurrences. One counter
 * serves every name group — sentinels are opaque, only the group membership
 * decides the substituted name.
 *
 * <p>The sentinel substitution is a literal {@link String#replace} of an
 * opaque generated token — not structural analysis of the generated Java.
 *
 * <p>Limitations (documented, no corpus carrier for any): a function
 * input/alias literally named like a group base ({@code ifThenElseResult},
 * {@code bigInteger}) would collide — upstream escapes {@code _name} via the
 * scope parent-walk; this count-based session does not (no frozen golden
 * exercises those names; the {@code _ifThenElseResult} forms that DO appear
 * in goldens are lambda-interior block-lambda carriers the fork declines).
 * The singleton arm's keyword escape (the {@code boolean} group's
 * {@code _boolean}) is the upstream {@code isName} loop, NOT that collision
 * parent-walk — symbol-name collisions with a group base remain unescaped
 * (and {@code boolean} itself cannot be a Rosetta symbol name colliding
 * here, being a Java-keyword-shaped base).
 * And a function mixing HOISTED conditionals with DECLINED ones
 * (lambda-interior, non-CR mid-expression, ADD) numbers only the hoisted
 * population, where upstream's group spans every collapsed conditional —
 * harmless because every declined conditional leaves an inline ternary,
 * which no golden carries, so such a file is a waivered mismatch regardless
 * of its numbering.
 *
 * <p>NOTE (PR #198 {@code ctorSingletonListHoist}): that facet
 * ({@code ConstructionHandler.hoistSingleValueIntoMultiOrNull}) is the FIRST
 * caller to {@link #register(String)} with a DYNAMIC base name — the lowercased
 * item-type simple name (e.g. {@code payout}) rather than a fixed constant
 * ({@link #IF_THEN_ELSE_RESULT}/{@link #BIG_INTEGER}/{@link #BOOLEAN}). The
 * symbol-name-collision limitation above is therefore now reachable in
 * principle (a ctor whose item type lowercases to the name of an enclosing
 * input/alias/output), though no corpus carrier hits it. A FUTURE facet that
 * hoists with a base name overlapping an in-scope symbol must add upstream's
 * {@code _name} parent-walk escape (the count-based session does not).
 */
public final class StatementHoistSession {

    /** The #173 if-then-else result-local name group. */
    public static final String IF_THEN_ELSE_RESULT = "ifThenElseResult";

    /** The biginteger_literal_hoist beyond-long literal name group. */
    public static final String BIG_INTEGER = "bigInteger";

    /**
     * The boolean_condition_hoist item-Boolean condition name group. The base
     * name is the upstream desired name ({@code Boolean.simpleName.toFirstLower}
     * from {@code convertNullSafe.declareAsVariable}) — a Java KEYWORD, so the
     * singleton arm of {@link #resolve(String)} escapes it to {@code _boolean};
     * numbered members ({@code boolean0..n-1}) are valid identifiers and never
     * escape ({@code _boolean0} has zero golden carriers).
     */
    public static final String BOOLEAN = "boolean";

    /**
     * The PR #98 then-chain re-rooting name group ({@code thenArg}/{@code thenArgN}). Unlike the
     * groups above, the then-chain does NOT register through this session —
     * {@code FunctionExpressionRenderer.renderThenExtractSet} names its {@code thenArg} locals
     * DIRECTLY (the chain length {@code n} is known immediately from the chain walk, so no
     * deferred-sentinel naming is needed; this session's count-based numbering MIRRORS that,
     * lifted to function scope — see the class docs). The constant lives here as the single
     * generator-side source of the literal (the D43 IR seam), shared by the renderer's and
     * {@code CollectionHandler}'s {@code thenArgBaseName} seams and the IR-routed overrides'
     * byte-identity guards (the IR side carries its own neutral {@code "thenArg"} lexeme in the
     * ANF normalizer — the override's guard asserts the two agree).
     */
    public static final String THEN_ARG = "thenArg";

    private static final AtomicInteger SENTINEL_COUNTER = new AtomicInteger();

    /** Registration-ordered sentinels per base name (insertion-ordered for determinism). */
    private final Map<String, List<String>> groups = new LinkedHashMap<>();

    /** Rung 2b (facet fnDeepCondBaseConfinedArmChainAdmit): after-next pendings - see
     * {@link #registerAfterNext}. */
    private final Map<String, List<String>> pendingAfterNext = new LinkedHashMap<>();

    /**
     * facet inLambdaArgSeatIteHoist (PR #355): per-LAMBDA sub-groups — base name →
     * (lambda key → registration-ordered sentinels). Upstream declares an in-lambda
     * hoist local in the LAMBDA's own scope, so the computeActualNames group law
     * applies per lambda: each lambda's group numbers independently (a singleton is
     * bare, n ≥ 2 number {@code base0..n-1} in registration order — golden GetOptn
     * esma: the first {@code mapSingleToItem} lambda carries
     * {@code ifThenElseResult0/1}, the sibling {@code mapItem} lambda restarts at
     * {@code ifThenElseResult0..3}), and every resolved name {@code _}-escapes
     * against the METHOD-level group's resolved names (golden mas
     * Create_ContractType15__1: the method's {@code ifThenElseResult0/1} force the
     * in-lambda pair to {@code _ifThenElseResult0/1}) — upstream's
     * escape-iff-ancestor-taken parent walk, applied per name.
     *
     * <p>The lambda KEY is the lambda-body compile scope's identity
     * ({@code JavaStatementScope.findPendingLambdaHoistBoundary()}), NOT the AST
     * node: a discarded compile attempt (the {@code compileLambda} block-form
     * attempts re-compile the same body in a sibling scope) keys its registrations
     * separately, leaving them as HARMLESS ghost sub-groups — their sentinels appear
     * in no surviving rendered text, groups number independently per key, and
     * {@link #resolve(String, java.util.Collection)}'s literal replace of an absent
     * token is a no-op — so no snapshot/restore is needed at the attempt sites.
     */
    private final Map<String, Map<Object, List<String>>> lambdaGroups = new LinkedHashMap<>();

    // facet aliasGeneralValueThenHoist (PR #350, F4): true for a session whose OWNER
    // discards the whole render on a null fall-through (the per-alias fresh sessions —
    // renderAliasThenHoistOrNull / renderAliasSinkHoistsOrNull). A mid-loop decline in
    // tryDeepThenHoist may strand already-registered sentinels in such a session (the
    // #250 k==0-only concern) without corrupting anything — the owner throws the session
    // away. The per-METHOD session stays non-discardable.
    private boolean discardableOwner;

    public void markDiscardableOwner() {
        this.discardableOwner = true;
    }

    public boolean isDiscardableOwner() {
        return discardableOwner;
    }

    // facet aliasGeneralValueThenHoist (PR #350, F4): set when a deep-then level emitted
    // a Mapper*<Object> decl (a form golden never carries) into a discardable session —
    // the alias seat discards the NEW general-value-arm render on it (legacy-arm renders
    // keep their pre-existing bytes).
    private boolean objectDecl;

    public void markObjectDecl() {
        this.objectDecl = true;
    }

    public boolean hasObjectDecl() {
        return objectDecl;
    }

    // facet aliasCondLevelObjectRecovery (PR #389): set when the tryDeepThenHoist
    // pre-scan EXEMPTED an Object-snapshot non-last cond level for this
    // discardable-owner session (the alias route) — the #351 handshake arm's
    // compiled-arm recovery runs instead. The alias seat's strand guard fires ONLY
    // for renders under this flag (a mid-loop decline leaves a partial restructure
    // whose consumer carries the runtime `.then(` marker); pre-existing partial
    // renders WITHOUT the flag keep their bytes (the NotionalLeg cp3-389 catch).
    private boolean condLevelObjectRecovery;

    public void markCondLevelObjectRecovery() {
        this.condLevelObjectRecovery = true;
    }

    public boolean hasCondLevelObjectRecovery() {
        return condLevelObjectRecovery;
    }

    /**
     * Register one hoisted local in the given name group and return the
     * sentinel token to embed everywhere its name belongs (declaration,
     * branch assignments, the consuming reference).
     *
     * @throws IllegalArgumentException if {@code baseName} is not
     *         identifier-shaped (a keyword like {@code boolean} IS — only
     *         illegal characters are rejected); guarantees the singleton
     *         keyword-escape loop in {@link #resolve(String)} terminates
     */
    public String register(String baseName) {
        if (!SourceVersion.isIdentifier(baseName)) {
            throw new IllegalArgumentException(
                    "hoist name-group base must be identifier-shaped: " + baseName);
        }
        String sentinel = "__STMT_HOIST_" + SENTINEL_COUNTER.getAndIncrement() + "__";
        groups.computeIfAbsent(baseName, k -> new ArrayList<>()).add(sentinel);
        List<String> pending = pendingAfterNext.remove(baseName);
        if (pending != null) {
            groups.get(baseName).addAll(pending);
        }
        return sentinel;
    }

    /**
     * facet fnDeepCondBaseConfinedArmChainAdmit (v3.1 flip seat 33, law D.3, rung 2b): the
     * AFTER-NEXT half of the {@link #mint}/{@link #attach} late-name law, for the OPPOSITE
     * emission order. A deep-seat conditional-BASE hoist admitted mid-compile of an
     * ENCLOSING SET level's value registers its {@code thenArg} BEFORE that level does
     * (renderThenExtractSetImpl registers a level only AFTER its value compiles, and this
     * compile IS the value) - but golden numbers the ENCLOSING level first (drr 7.x
     * TotalNotionalQuantity: outer {@code thenArg1}, interior {@code thenArg2}; upstream
     * creates the outer declaration's identifier before compiling its initializer). The
     * token minted here joins {@code baseName}'s group immediately AFTER the NEXT
     * {@link #register} on the same base; if no register follows (the enclosing render was
     * discarded), it attaches at the group's tail before any {@link #resolve} /
     * {@link #resolvedNames} read. Pending tokens are invisible to {@link #methodGroupSize}
     * (every pre-existing read keeps its exact semantics) and, while still pending, are
     * covered by {@link #snapshot}/{@link #restore} under a NUL-prefixed key (a base name is
     * identifier-shaped, so the prefix cannot collide). DISCLOSED, not guarded: a pending
     * consumed by an intervening {@link #register} on the same base is attached, and
     * {@link #restore} cannot detach it again; and a pending that reaches the tail flush of
     * a SINGLETON group inflates that group and renumbers its survivor. Neither shape occurs
     * at this corpus (band 0 at the seat's head, both routes).
     */
    public String registerAfterNext(String baseName) {
        if (!SourceVersion.isIdentifier(baseName)) {
            throw new IllegalArgumentException(
                    "hoist name-group base must be identifier-shaped: " + baseName);
        }
        String sentinel = "__STMT_HOIST_" + SENTINEL_COUNTER.getAndIncrement() + "__";
        pendingAfterNext.computeIfAbsent(baseName, k -> new ArrayList<>()).add(sentinel);
        return sentinel;
    }

    /** Rung 2b: append every still-pending after-next token to its group's tail. */
    private void flushPendingAfterNext() {
        for (Map.Entry<String, List<String>> e : pendingAfterNext.entrySet()) {
            groups.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).addAll(e.getValue());
        }
        pendingAfterNext.clear();
    }

    /**
     * facet ctorSetterArgIteHoist (PR #397): mint a sentinel token WITHOUT attaching it
     * to a name group — the LATE-NAME half of the #392/#395 law at the pathed-SET seat.
     * The pathed outer conditional's {@code ifThenElseResult} local is CONSUMED before
     * its arm compiles but NUMBERED after the arm's interior hoists (golden AnnaDsb
     * BaseProductForCommodity: interiors 0..7, outer 8) — the caller mints the token up
     * front (the decl text needs it) and {@link #attach}es it to the group AFTER the
     * arm compile, so {@link #resolve}'s registration-order numbering lands the golden
     * order. A minted-but-never-attached token would survive resolve() verbatim (a
     * non-compiling identifier) — every caller must attach on all paths.
     */
    public String mint() {
        return "__STMT_HOIST_" + SENTINEL_COUNTER.getAndIncrement() + "__";
    }

    /**
     * facet ctorSetterArgIteHoist (PR #397): attach a {@link #mint}ed token to
     * {@code baseName}'s group at the CURRENT registration position.
     */
    public void attach(String baseName, String sentinel) {
        if (!SourceVersion.isIdentifier(baseName)) {
            throw new IllegalArgumentException(
                    "hoist name-group base must be identifier-shaped: " + baseName);
        }
        groups.computeIfAbsent(baseName, k -> new ArrayList<>()).add(sentinel);
    }

    /**
     * facet inLambdaArgSeatIteHoist (PR #355): register one hoisted local in the
     * per-lambda sub-group of {@code baseName} keyed by {@code lambdaKey} (the
     * enclosing lambda-body compile scope — see the {@code lambdaGroups} javadoc)
     * and return the sentinel token. Registration order within one (base, key)
     * sub-group = consumption order (an arm-interior conditional registers DURING
     * the outer's arm compile, so inner numbers before outer — the #327
     * consumption-order law falling out of the call order).
     */
    public String registerLambdaScoped(String baseName, Object lambdaKey) {
        if (!SourceVersion.isIdentifier(baseName)) {
            throw new IllegalArgumentException(
                    "hoist name-group base must be identifier-shaped: " + baseName);
        }
        String sentinel = "__STMT_HOIST_" + SENTINEL_COUNTER.getAndIncrement() + "__";
        lambdaGroups.computeIfAbsent(baseName, k -> new LinkedHashMap<>())
                .computeIfAbsent(lambdaKey, k -> new ArrayList<>()).add(sentinel);
        return sentinel;
    }

    /** True when no hoist registered — {@link #resolve(String)} would be a no-op. */
    public boolean isEmpty() {
        return groups.isEmpty() && lambdaGroups.isEmpty();
    }

    /**
     * Capture the current per-group registration counts so a discarded render's
     * registrations can be rolled back via {@link #restore(Map)}.
     *
     * <p>The renderThenExtractSet cascade fallback (PR #257) renders a then-chain
     * twice — once WITH the ite-hoist (discarded if it carries a runtime
     * {@code .then(} chain), once WITHOUT — and the per-method session is a FIELD, not
     * scope-local, so the discarded first render's {@code thenArg} /
     * {@code ifThenElseResult} registrations would otherwise inflate the group
     * counts and renumber the kept render's locals (the latent double-register the
     * #257 caveat documents). Snapshot before the first render; {@link #restore}
     * before re-rendering. Harmless on the FUNCTION path historically (no carrier
     * reached the fallback with a session-registering body) and load-bearing now
     * the session is enabled on the RULE path (PR #262) — every drr Rule cascade
     * carrier double-renders.
     */
    public Map<String, Integer> snapshot() {
        Map<String, Integer> snap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            snap.put(e.getKey(), e.getValue().size());
        }
        // Rung 2b: after-next pendings under a NUL-prefixed key (never identifier-shaped).
        for (Map.Entry<String, List<String>> e : pendingAfterNext.entrySet()) {
            snap.put("\u0000pending:" + e.getKey(), e.getValue().size());
        }
        return snap;
    }

    /**
     * Roll back to a {@link #snapshot()}: truncate each group to its captured size
     * and drop any group created since the snapshot. The global sentinel counter is
     * NOT rewound (sentinels are opaque; gaps in the counter never matter — only
     * group membership + intra-group order decide the {@link #resolve(String)}
     * names), so a restored session resumes registration cleanly.
     */
    public void restore(Map<String, Integer> snap) {
        groups.entrySet().removeIf(e -> !snap.containsKey(e.getKey()));
        pendingAfterNext.entrySet().removeIf(
                e -> !snap.containsKey("\u0000pending:" + e.getKey()));
        for (Map.Entry<String, Integer> e : snap.entrySet()) {
            boolean pendingKey = e.getKey().startsWith("\u0000pending:");
            List<String> list = pendingKey
                    ? pendingAfterNext.get(e.getKey().substring("\u0000pending:".length()))
                    : groups.get(e.getKey());
            if (list != null) {
                while (list.size() > e.getValue()) {
                    list.remove(list.size() - 1);
                }
            }
        }
    }

    /**
     * Substitute every registered sentinel with its group-law name: the bare
     * base name for a singleton group, {@code base0..n-1} in registration
     * order otherwise; each group resolves independently.
     *
     * <p>facet addHoistLocalParamEscape (PR #347): both arms now ALSO
     * {@code _}-escape while the resolved name is in {@code takenNames} (the
     * method's seeded inputs/output/shortcuts + dependency fields) — upstream's
     * {@code computeActualNames} escape-iff-taken loop, applied at the group
     * replay exactly like the keyword escape. Golden cdm6 CalculateTransfer
     * pins the arm ORDER (number FIRST, then escape): its two {@code transfer}
     * hoists number to {@code transfer0/transfer1} — no escape despite the
     * colliding {@code transfer} output param — while golden cdm6
     * MapAveragingObservations' SINGLETON {@code averagingObservationList}
     * escapes to {@code _averagingObservationList} under the same-named input
     * param (a bare local shadowing a method param is illegal Java, so every
     * colliding singleton carrier was an already-waivered non-compiling
     * mismatch; non-colliding locals resolve identically to the pre-facet law).
     */
    public String resolve(String src) {
        // The pre-#347 law (keyword-only escape) for the interior/rule-path resolve
        // seats — no taken-name evidence there; bytes preserved.
        return resolve(src, null);
    }

    public String resolve(String src, java.util.Collection<String> takenNames) {
        return resolve(src, takenNames, java.util.Set.of());
    }

    /**
     * facet condDerefTextOrderUnify (PR #378): {@code skipBases} variant — method-level
     * groups named in {@code skipBases} are NOT resolved here; their sentinels ride into
     * {@code JavaStatementScope.resolveUnifiedDeferredNames}' replay, where they number
     * as ONE text-ordered group with the same-base surviving deferred coercion entries
     * (the #329 documented limitation's fix — carrier CallQuantity/PutQuantity). The
     * in-lambda escape below still reads the SKIPPED group's session-law names for its
     * unusable set (no corpus carrier pairs an in-lambda sub-group with a unified base;
     * a future one would need the replay-resolved names threaded here).
     */
    public String resolve(String src, java.util.Collection<String> takenNames,
            java.util.Set<String> skipBases) {
        flushPendingAfterNext();
        if (groups.isEmpty() && lambdaGroups.isEmpty()) {
            return src;
        }
        String out = src;
        for (Map.Entry<String, List<String>> group : groups.entrySet()) {
            String baseName = group.getKey();
            if (skipBases.contains(baseName)) {
                continue;
            }
            List<String> sentinels = group.getValue();
            if (sentinels.size() == 1) {
                // A singleton group's bare base name may be a Java keyword
                // ('boolean') — escape with the upstream GeneratorScope
                // '_'-prefix loop (numbered members are valid identifiers and
                // rarely escape below). Terminates by register()'s
                // identifier-shape precondition: an identifier-shaped non-name
                // is a keyword, valid after one '_' prefix; a taken name is
                // freed by at most len(takenNames) prefixes.
                out = out.replace(sentinels.get(0), escapeWhileUnusable(baseName, takenNames));
            } else {
                for (int i = 0; i < sentinels.size(); i++) {
                    out = out.replace(sentinels.get(i),
                            escapeWhileUnusable(baseName + i, takenNames));
                }
            }
        }
        // facet inLambdaArgSeatIteHoist (PR #355): each lambda sub-group resolves
        // by the SAME group law independently per lambda key, then every name
        // _-escapes against the METHOD-level same-base group's resolved names
        // (the upstream ancestor-taken walk: an in-lambda `ifThenElseResult0`
        // under method-level `ifThenElseResult0/1` becomes `_ifThenElseResult0`
        // — golden mas Create_ContractType15__1). Sibling lambdas' sub-groups
        // deliberately do NOT escape against each other (disjoint Java scopes —
        // golden GetOptn esma carries `ifThenElseResult0` in BOTH sibling
        // lambdas). Ghost sub-groups (discarded compile attempts) resolve to
        // no-op replaces of tokens absent from {@code src}.
        for (Map.Entry<String, Map<Object, List<String>>> lambdaGroup : lambdaGroups.entrySet()) {
            String baseName = lambdaGroup.getKey();
            java.util.Set<String> unusable = new java.util.LinkedHashSet<>();
            List<String> methodGroup = groups.get(baseName);
            if (methodGroup != null) {
                if (methodGroup.size() == 1) {
                    unusable.add(escapeWhileUnusable(baseName, takenNames));
                } else {
                    for (int i = 0; i < methodGroup.size(); i++) {
                        unusable.add(escapeWhileUnusable(baseName + i, takenNames));
                    }
                }
            }
            if (takenNames != null) {
                unusable.addAll(takenNames);
            }
            for (List<String> sentinels : lambdaGroup.getValue().values()) {
                if (sentinels.size() == 1) {
                    out = out.replace(sentinels.get(0), escapeWhileUnusable(baseName, unusable));
                } else {
                    for (int i = 0; i < sentinels.size(); i++) {
                        out = out.replace(sentinels.get(i),
                                escapeWhileUnusable(baseName + i, unusable));
                    }
                }
            }
        }
        return out;
    }

    /** The upstream computeActualNames escape loop: {@code _}-prefix while a keyword or taken. */
    private static String escapeWhileUnusable(String name, java.util.Collection<String> takenNames) {
        String result = name;
        while (!SourceVersion.isName(result)
                || (takenNames != null && takenNames.contains(result))) {
            result = "_" + result;
        }
        return result;
    }

    /**
     * facet lambdaNaming M3/M4 (PR #329): every resolved hoist-local name this
     * session emits ({@link #resolve} group law — the bare/keyword-escaped base
     * for a singleton, {@code base0..n-1} otherwise). Seeded into the unified
     * method naming scope ({@code JavaStatementScope.resolveUnifiedDeferredNames})
     * so a deferred lambda param escapes against a session hoist local exactly as
     * upstream's single method scope resolves it — the local is a method-level
     * identifier there regardless of WHEN it was registered (golden
     * {@code _dateTimeList} against the {@code dateTimeList} ctor-hoist local).
     */
    public List<String> resolvedNames(java.util.Collection<String> takenNames) {
        return resolvedNames(takenNames, java.util.Set.of());
    }

    /**
     * facet condDerefTextOrderUnify (PR #378): {@code skipBases} variant — a group the
     * unified replay owns contributes NO seed name here (its members number WITH the
     * same-base deferred coercion entries inside the replay, so seeding its session-law
     * names would re-introduce the very {@code _}-escape the unify removes).
     */
    public List<String> resolvedNames(java.util.Collection<String> takenNames,
            java.util.Set<String> skipBases) {
        flushPendingAfterNext();
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, List<String>> group : groups.entrySet()) {
            String baseName = group.getKey();
            if (skipBases.contains(baseName)) {
                continue;
            }
            List<String> sentinels = group.getValue();
            if (sentinels.size() == 1) {
                names.add(escapeWhileUnusable(baseName, takenNames));
            } else {
                for (int i = 0; i < sentinels.size(); i++) {
                    names.add(escapeWhileUnusable(baseName + i, takenNames));
                }
            }
        }
        return names;
    }

    /**
     * facet condDerefTextOrderUnify (PR #378): the method-level group base names, in
     * registration order — the unify gate's candidate set (structural bases are
     * excluded at the gate site).
     */
    public java.util.Set<String> methodGroupBaseNames() {
        return new java.util.LinkedHashSet<>(groups.keySet());
    }

    /**
     * facet condDerefTextOrderUnify (PR #378): the (base → registration-ordered
     * sentinels) view of the given method-level groups for the unified-replay merge.
     * Copies — the session structures stay intact.
     */
    public Map<String, List<String>> methodGroupsFor(java.util.Set<String> bases) {
        Map<String, List<String>> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            if (bases.contains(e.getKey())) {
                out.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }
        return out;
    }

    /**
     * facet inLambdaCondBaseArmChainDecomp (PR #381, D4b): the METHOD group's member
     * count for {@code baseName} — the in-lambda session-continue discriminator reads
     * it ({@code >= 2} means the group resolves NUMBERED, so an in-lambda
     * statement-seat hoist of a conditional-base chain at the lambda-body root JOINS
     * the group instead of taking the per-scope deferred channel). Lambda sub-groups
     * are deliberately not counted — they number per lambda key.
     */
    public int methodGroupSize(String baseName) {
        List<String> group = groups.get(baseName);
        return group == null ? 0 : group.size();
    }
}
