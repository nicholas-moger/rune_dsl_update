package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.ast.types.RTypeAlias;

/**
 * Resolves {@link SymbolId} opaque handles to their target {@link RNode}.
 * Implemented by {@link RWorkspace} in production; tests use a lightweight
 * {@code TestSymbolResolver} that bypasses workspace construction.
 *
 * <p>Extracted to allow {@link RNode}'s WeakReference back-pointer to be
 * pluggable across production + test paths without de-finalising
 * {@code RWorkspace}.
 *
 * <p>Per H7 / U005.
 */
public interface SymbolResolver {

    /**
     * Sentinel namespace string for builtin types ({@code string}, {@code int},
     * {@code RDate}, etc.) that live in
     * {@code RFileScope.builtinNamespace()} rather than the user-facing
     * {@code namespaces} map. SymbolIds emitted for resolved builtin targets
     * carry this namespace.
     *
     * <p>Implementations of {@link #resolve} must route this sentinel to the
     * workspace's builtin namespace. Production: {@code RWorkspace.resolve}
     * scans {@code fileScopes.values().stream().map(builtinNamespace).filter(non-null)}
     * (added by Task 2 of the P1.4.1b plan). Test path: builtins can be
     * registered explicitly via {@code TestSymbolResolver.bind} using
     * {@code BUILTIN_NAMESPACE} as the namespace argument — the test resolver
     * stores under any namespace key uniformly.
     *
     * <p>Per SF8 from reviewer findings on the original P1.4.1b plan.
     */
    String BUILTIN_NAMESPACE = "<builtin>";

    /**
     * Returns this resolver's generation token. Stable for the resolver's
     * lifetime; mismatched generations on {@link #resolve(SymbolId, Class)}
     * surface as {@link StaleSymbolIdException}.
     */
    long generation();

    /**
     * Resolves a {@link SymbolId} to its target node.
     *
     * <p>Behaviour (implementations MUST honour this exact ordering — the
     * {@code expected} non-null check wins over generation/staleness checks
     * so a fully-bogus call surfaces as a clean NPE rather than being
     * silently swallowed by a paired-null code path):
     * <ol>
     *   <li>{@code expected == null} → {@link NullPointerException}.</li>
     *   <li>{@code id == null} → returns {@code null}.</li>
     *   <li>{@code id.generation() != this.generation()} →
     *       {@link StaleSymbolIdException}.</li>
     *   <li>Unknown namespace OR unknown local name → returns {@code null}.</li>
     *   <li>Resolved target's runtime class is not assignable to
     *       {@code expected} → {@link IllegalArgumentException}.</li>
     *   <li>Otherwise → resolved target cast to {@code T}.</li>
     * </ol>
     *
     * @throws NullPointerException if {@code expected} is null
     */
    <T extends RNode> T resolve(SymbolId id, Class<T> expected);

    /**
     * Resolves a {@link SymbolId} known to refer to a type-like declaration
     * (data type, enum, choice, basic, record, or type alias), bypassing
     * first-wins lookup on shared local names.
     *
     * <p>Unlike {@link #resolve(SymbolId, Class)} — which calls
     * {@code ns.lookup(localName)} and returns whichever element was registered
     * first — this method walks every declaration matching the local name and
     * returns the first one that is type-like. This matters when a rule, function,
     * or shortcut shares a local name with a type in the same namespace
     * (e.g. {@code reporting rule EnrichmentData} and {@code type EnrichmentData}
     * both in {@code drr.enrichment.common}): the type-position SymbolId built by
     * {@link com.regnosys.rosetta.symbols.linker.GlobalResolutionPass#resolveTypeCall}
     * must re-resolve to the type, not the rule.
     *
     * <p>Implementations: production walks {@code ns.allMatching(localName)} +
     * filters via the {@link #isTypeLike(RRootElement)} predicate exposed below.
     * The default here resolves to an {@code RNode} via {@link #resolve(SymbolId,
     * Class)} and returns it only if it is an {@link RRootElement} satisfying
     * {@link #isTypeLike(RRootElement)} — otherwise returns {@code null}. This
     * matches the strict contract even for resolvers that don't model rule/type
     * name collisions: a test resolver registering only a type at a given
     * {@code SymbolId} returns the type; a test resolver registering a non-type
     * (rule / function / shortcut) returns {@code null} so the type-position
     * caller observes the gap explicitly instead of silently binding to the
     * wrong kind (Copilot PR #76 R18 F1 — earlier default delegated to
     * {@code resolve(id, RNode.class)} with no kind filter, which contradicted
     * this javadoc and could reintroduce the rule-vs-type collision bug for
     * test resolvers).
     *
     * @return the resolved type-like target, or {@code null} if {@code id} is
     *     null, the namespace is unknown, no match exists, or the match is not
     *     a type-like {@link RRootElement}
     */
    default RNode resolveTypeLike(SymbolId id) {
        RNode target = resolve(id, RNode.class);
        if (target instanceof RRootElement re && isTypeLike(re)) {
            return target;
        }
        return null;
    }

    /**
     * PR #445 — requester-aware variant of {@link #resolve(SymbolId, Class)}.
     * A {@link SymbolId} is name-based, so a lazy accessor re-resolving it
     * cannot express WHICH declaration it meant when a merged multi-closure
     * workspace carries duplicate FQNs (the cdm-5.38.0 / cdm-6.20.6 cell
     * collision). Passing the resolving node lets the production resolver
     * apply the {@link CellPreference} rank (same file, same cell,
     * registration order). The default ignores the requester — resolvers
     * without collision handling keep their exact prior semantics.
     */
    default <T extends RNode> T resolve(SymbolId id, Class<T> expected, RNode requester) {
        return resolve(id, expected);
    }

    /**
     * PR #445 — requester-aware variant of {@link #resolveTypeLike(SymbolId)};
     * same contract, same {@link CellPreference} rationale as
     * {@link #resolve(SymbolId, Class, RNode)}. The default ignores the
     * requester.
     */
    default RNode resolveTypeLike(SymbolId id, RNode requester) {
        return resolveTypeLike(id);
    }

    /**
     * Predicate matching the set of {@link RRootElement} kinds that may
     * legitimately appear in a type position (i.e. the targets that
     * {@link #resolveTypeLike(SymbolId)} returns). Shared canonical
     * implementation — production resolvers (e.g. {@code RWorkspace})
     * filter {@code ns.allMatching} results through this predicate. Update
     * this method AND the {@link #resolveTypeLike(SymbolId)} javadoc above
     * in sync when adding new RNode kinds.
     */
    static boolean isTypeLike(RRootElement node) {
        return node instanceof RDataType
            || node instanceof REnumeration
            || node instanceof RChoice
            || node instanceof RBasicType
            || node instanceof RRecordType
            || node instanceof RTypeAlias;
    }
}
