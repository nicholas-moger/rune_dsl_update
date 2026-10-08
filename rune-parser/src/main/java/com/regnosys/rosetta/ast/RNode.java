package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.symbols.SymbolResolver;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class for all AST nodes in the Rune DSL typed AST.
 *
 * <p>Every node carries:
 * <ul>
 *   <li>A {@link SourceRange} for the overall node span.</li>
 *   <li>A map of token-level {@code SourceRange}s keyed by token name
 *       (e.g., "keyword", "name", "colon").</li>
 *   <li>A nullable parent reference, set post-construction during tree wiring.</li>
 * </ul>
 *
 * <p><strong>Mutation contract:</strong> AST nodes are mutable during construction
 * (the {@code AstBuilder} sets fields and lists via setters and direct list mutation).
 * After construction and parent-wiring, nodes are treated as effectively immutable.
 * List fields are exposed directly (not defensively copied) for builder convenience.
 * The only post-construction mutation is M3's derived state computer, which fills
 * well-defined optional fields (e.g., implicit variable, default else).
 *
 * <p>The {@link #children()} method is the primary mechanism for tree traversal
 * (see Task 15 — visitor / walker infrastructure). Every concrete node class
 * must return its child nodes from this method. By default it returns an empty list,
 * which is correct for leaf nodes.
 */
public abstract class RNode {

    private static final Map<String, SourceRange> EMPTY_TOKEN_RANGES = Collections.emptyMap();
    private static final Map<String, Object> EMPTY_METADATA = Map.of();

    private SourceRange sourceRange = SourceRange.NONE;
    private Map<String, SourceRange> tokenRanges = EMPTY_TOKEN_RANGES;
    private RNode parent;
    private final Map<String, Object> metadata;
    private boolean frozen = false;
    private WeakReference<SymbolResolver> workspaceRef;

    // -- Constructors ---------------------------------------------------------

    /**
     * Default constructor — empty metadata, {@link SourceRange#NONE}.
     * Existing subclasses that don't call {@code super(...)} explicitly
     * continue to compile against this signature.
     */
    protected RNode() {
        this.metadata = EMPTY_METADATA;
    }

    /**
     * Source-range-only constructor — empty metadata.
     */
    protected RNode(SourceRange range) {
        this.sourceRange = Objects.requireNonNull(range,
                "sourceRange must not be null; use SourceRange.NONE for unknown locations");
        this.metadata = EMPTY_METADATA;
    }

    /**
     * Full constructor — both source range and metadata. The metadata map is
     * defensively copied at construction; the resulting field is unmodifiable.
     * A {@code null} metadata argument is coerced to an empty map.
     */
    protected RNode(SourceRange range, Map<String, Object> metadata) {
        this.sourceRange = Objects.requireNonNull(range,
                "sourceRange must not be null; use SourceRange.NONE for unknown locations");
        this.metadata = (metadata == null || metadata.isEmpty())
                ? EMPTY_METADATA
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    // -- Source range ----------------------------------------------------------

    /**
     * Returns the source range covering this entire node.
     * Defaults to {@link SourceRange#NONE} until set by the AST builder.
     */
    public SourceRange sourceRange() {
        return sourceRange;
    }

    /**
     * Sets the source range. Must not be {@code null} — pass
     * {@link SourceRange#NONE} for "unknown location" instead. Enforced to
     * preserve the documented invariant that {@link #sourceRange()} always
     * returns a non-null value.
     */
    public void setSourceRange(SourceRange sourceRange) {
        checkMutable();
        this.sourceRange = Objects.requireNonNull(sourceRange,
                "sourceRange must not be null; use SourceRange.NONE for unknown locations");
    }

    // -- Token ranges ---------------------------------------------------------

    /**
     * Returns an unmodifiable view of token-level source ranges.
     * Keys are token names (e.g., "keyword", "name"); values are their
     * locations in the source file.
     */
    public Map<String, SourceRange> tokenRanges() {
        return Collections.unmodifiableMap(tokenRanges);
    }

    /**
     * Replaces all token ranges with a copy of the given map. Must not be
     * {@code null} — pass an empty map to clear. Enforced to preserve the
     * documented invariant that {@link #tokenRanges()} always returns a
     * non-null map.
     */
    public void setTokenRanges(Map<String, SourceRange> tokenRanges) {
        checkMutable();
        Objects.requireNonNull(tokenRanges, "tokenRanges must not be null; use an empty map to clear");
        // Validation order: checkMutable -> requireNonNull(map) -> per-entry
        // validation -> defensive copy. Frozen-error always wins over
        // null-content errors because a frozen node MUST reject any
        // mutation regardless of its arg shape; once we're past freeze,
        // per-entry validation matches putTokenRange's contract that token
        // names + ranges must be non-null per the documented
        // tokenRanges() invariant. The LinkedHashMap copy is the last
        // statement, so a mid-iteration NPE leaves the node unchanged.
        for (Map.Entry<String, SourceRange> entry : tokenRanges.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "tokenRanges keys (token names) must not be null");
            Objects.requireNonNull(entry.getValue(),
                    "tokenRanges values (ranges) must not be null; use SourceRange.NONE for unknown locations");
        }
        this.tokenRanges = new LinkedHashMap<>(tokenRanges);
    }

    /**
     * Adds a single token range entry. Lazily promotes the internal map
     * from the empty singleton to a mutable {@link LinkedHashMap}. Both
     * arguments must be non-null — token names and ranges are part of the
     * documented {@link #tokenRanges()} contract.
     */
    public void putTokenRange(String tokenName, SourceRange range) {
        checkMutable();
        Objects.requireNonNull(tokenName, "tokenName must not be null");
        Objects.requireNonNull(range, "range must not be null; use SourceRange.NONE for unknown locations");
        if (tokenRanges == EMPTY_TOKEN_RANGES) {
            tokenRanges = new LinkedHashMap<>();
        }
        tokenRanges.put(tokenName, range);
    }

    // -- Parent ---------------------------------------------------------------

    /**
     * Returns the parent node, or {@code null} for the root of the tree.
     * Set post-construction during tree wiring.
     */
    public RNode parent() {
        return parent;
    }

    public void setParent(RNode parent) {
        checkMutable();
        this.parent = parent;
    }

    // -- Children (abstract for traversal) ------------------------------------

    /**
     * Returns the direct child nodes of this AST node.
     *
     * <p>This method is the foundation for tree traversal (visitors, walkers).
     * Concrete node classes must override this to return their structural children.
     * Leaf nodes inherit the default empty-list implementation.
     *
     * <p><strong>Mutation contract:</strong> callers MUST treat the returned
     * list as read-only. Implementations may return either an unmodifiable
     * view or the underlying internal list directly (per the class-level
     * "list fields exposed directly" note).
     *
     * <p><strong>Scope of {@link #freeze()} (D12):</strong> the freeze flag
     * guards only the setter methods on this class and its subclasses (every
     * setter calls {@link #checkMutable()} as its first statement). It does
     * NOT prevent mutation of the collections returned by {@code children()}
     * or by sibling collection-returning getters such as
     * {@code RDataType.attributes()}. Those getters typically expose the
     * underlying {@code List}/{@code ArrayList} for builder convenience, so a
     * caller that does {@code node.attributes().add(...)} can still mutate
     * post-freeze without hitting {@code checkMutable()}. Wrapping every
     * collection getter in an unmodifiable view at freeze time is deferred
     * to a future PR (records-everywhere aspiration in P2 — see
     * {@code docs/upgrades/U002-frozen-ast-after-linker.md} §"Known scope
     * limitations"). Until then: treat returned collections as read-only.
     *
     * @return a non-null list of child nodes; treat as read-only
     */
    public List<? extends RNode> children() {
        return Collections.emptyList();
    }

    // -- Freeze flag (D12 / U002) ---------------------------------------------

    /**
     * Freezes this node and recursively freezes all reachable children.
     * Idempotent — second call is a no-op. Once frozen, every setter on this
     * node throws {@link IllegalStateException}.
     *
     * <p>Called by {@code RWorkspace.build(...)} after pass 7 (semantic
     * validation) completes. External callers should not invoke this. Per
     * D12 in the development decision log.
     */
    public void freeze() {
        if (frozen) return;
        frozen = true;
        for (RNode child : children()) {
            if (child != null) child.freeze();
        }
    }

    /**
     * @return true iff {@link #freeze()} has been called on this node.
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Guard for setters. Throws {@link IllegalStateException} if this node
     * has been frozen. Hard exception (not {@code assert}) so consumer JVMs
     * without {@code -ea} still get the protection.
     *
     * <p>Subclass setters MUST call this as their first statement.
     */
    protected final void checkMutable() {
        if (frozen) {
            throw new IllegalStateException(
                    "RNode mutated post-freeze: " + getClass().getSimpleName()
                    + " at " + sourceRange);
        }
    }

    // -- Metadata --------------------------------------------------------------

    /**
     * Returns an unmodifiable view of the node's metadata map. Empty by
     * default; populated by future-phase passes (D21 IR augmentation hook).
     * Always non-null.
     *
     * <p>Per H6 / U003 in {@code docs/upgrades/U003-rnode-metadata-slot.md}.
     */
    public Map<String, Object> metadata() {
        return metadata;
    }

    // -- Workspace back-pointer (H7 / U005) ------------------------------------

    /**
     * Attaches this node to a {@link SymbolResolver}. Stored as a
     * {@link WeakReference} — the resolver can still be garbage-collected if
     * all other references drop, in which case {@link #workspace()} throws
     * {@link IllegalStateException} indicating the cross-ref resolution path
     * is no longer available.
     *
     * <p>In the production lifecycle, {@code RWorkspace.build(...)} attaches
     * each node twice: first after pass 4 to an interim / in-build resolver
     * (so passes 5–7 and any lazy {@code superType()} / {@code referencedType()}
     * resolution they trigger have a working back-pointer), then again after
     * the final {@code RWorkspace} instance is constructed. Both attachment
     * points occur before {@link #freeze()}; see the lifecycle comments and
     * {@code Reference.reachabilityFence} call in {@code RWorkspace.build(...)}
     * for the authoritative sequencing. Direct-construction tests use
     * {@code TestSymbolResolver} (see {@link SymbolResolver} javadoc).
     * Re-invocations on the same node overwrite the previous attachment.
     *
     * <p>Visibility is {@code public} so test classes in any package (e.g.
     * {@code com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver}) can
     * attach manually-constructed nodes from any AST package (e.g. nodes in
     * {@code com.regnosys.rosetta.ast.types}). Tightening to {@code protected}
     * would break that cross-package test path.
     *
     * <p><b>Intentionally NOT guarded by {@link #checkMutable()}.</b> The lifecycle
     * is: pass 4 (interim attach to in-build resolver) → pass 5-7 → re-attach to
     * the constructed workspace → freeze. Both attach calls happen BEFORE freeze
     * for any given node, so guarding with checkMutable would never reject a
     * legitimate call. The deliberate gap also permits future incremental-compilation
     * scenarios (P1.4.4) where a frozen node may be re-attached to a successor
     * workspace without re-allocation. Re-attach replaces the prior WeakReference
     * atomically — no leak.
     *
     * <p>Per H7 / U005.
     */
    public void attachToWorkspace(SymbolResolver resolver) {
        Objects.requireNonNull(resolver, "resolver must not be null");
        this.workspaceRef = new WeakReference<>(resolver);
    }

    /**
     * Hands this node's resolver to a SYNTHETIC node built from it, so that an id the synthetic carries
     * resolves through the same workspace — ONE declaration for every synthetic builder (v3.2 seat 4,
     * PR #625, round 1: hoisted here from {@code RReport}, LAW 69; {@code RFunction.fromReport} consults it,
     * the rule and report generators' synthetic type calls are attached with the workspace in hand).
     * Returns whether the resolver was shared: a node that has none (never attached, or its workspace
     * collected) shares nothing and returns {@code false}, and the caller must then leave the synthetic
     * id-LESS — an id on a detached node is exactly the state whose first {@code referencedType()} read
     * threw "never attached" and was rendered as a TODO comment (the seat's one surprise); id-less, the
     * consumer REFUSES loudly instead. Round 2 (the code-quality review's SF-5 / NIT-6): the weak reference is
     * read ONCE — a has-then-get pair could see the resolver collected between its two reads and throw from a
     * method documented to return {@code false} — so the contract holds by construction; a null synthetic is
     * refused up front rather than answering {@code false} on a detached node and throwing on an attached one.
     */
    public final boolean shareResolverWith(RNode synthetic) {
        Objects.requireNonNull(synthetic, "synthetic must not be null");
        WeakReference<SymbolResolver> ref = this.workspaceRef;
        SymbolResolver resolver = ref == null ? null : ref.get();
        if (resolver == null) {
            return false;
        }
        synthetic.attachToWorkspace(resolver);
        return true;
    }

    /**
     * Returns the {@link SymbolResolver} this node was attached to. Method
     * name is "workspace" (rather than "resolver") to match the production
     * call-site idiom — production callers attach an {@link
     * com.regnosys.rosetta.symbols.RWorkspace}, which is the resolver.
     *
     * <p>Distinguishes "never attached" vs "attached but GC'd" in the error
     * message — these are operationally different states (former implies an
     * attach-traversal bug or test-setup gap; latter implies a workspace
     * lifecycle leak), so triage benefits from telling them apart.
     *
     * <p>Per H7 / U005.
     *
     * @throws IllegalStateException if this node was never attached, or if
     *     the attached resolver has been garbage-collected
     */
    protected final SymbolResolver workspace() {
        WeakReference<SymbolResolver> ref = this.workspaceRef;
        if (ref == null) {
            throw new IllegalStateException(
                    "RNode " + getClass().getSimpleName() + " at " + sourceRange
                    + " was never attached to a workspace; cross-reference resolution unavailable");
        }
        SymbolResolver resolver = ref.get();
        if (resolver == null) {
            throw new IllegalStateException(
                    "RNode " + getClass().getSimpleName() + " at " + sourceRange
                    + " was attached but the workspace has been garbage-collected;"
                    + " cross-reference resolution unavailable");
        }
        return resolver;
    }
}
