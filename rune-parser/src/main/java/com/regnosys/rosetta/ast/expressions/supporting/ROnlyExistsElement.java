package com.regnosys.rosetta.ast.expressions.supporting;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;

import java.util.ArrayList;
import java.util.List;

/**
 * Only-exists element node, corresponding to the {@code onlyExistsElement}
 * grammar rule.
 *
 * <p>Represents a single element in an {@code only-exists} expression.
 * Each element has a root (a qualified name or {@code item}) and an optional
 * chain of feature path segments.
 *
 * <p>Grammar:
 * <pre>
 * onlyExistsElement:
 *     (qualifiedName | ITEM) (ARROW validID)*
 * ;
 * </pre>
 */
public class ROnlyExistsElement extends RNode {

    /**
     * Defensive bound on the {@link #encloses} parent walk (cyclic parent pointers should
     * never occur) — mirrors {@code ExpressionToIRAdapter.PARENT_WALK_LIMIT}, the bound the IR
     * adapter's own ancestor walk carried before it consulted this declaration (v3.2 seat 8
     * round 1: the ONE read keeps the strictest bound any consumer had, so the adapter's swap
     * is an identity again and the three unbounded consumers narrow only past depth 64, a
     * depth no only-exists path reaches).
     */
    private static final int ENCLOSURE_WALK_LIMIT = 64;

    private String root;
    private boolean rootIsItem;
    private final List<String> featureChain = new ArrayList<>();
    private RExpression receiverExpression;
    private RFeatureCall leafReference;

    // -- root -----------------------------------------------------------------

    public String root() {
        return root;
    }

    public void setRoot(String root) {
        checkMutable();
        this.root = root;
    }

    // -- rootIsItem -----------------------------------------------------------

    public boolean isRootItem() {
        return rootIsItem;
    }

    public void setRootIsItem(boolean rootIsItem) {
        checkMutable();
        this.rootIsItem = rootIsItem;
    }

    // -- featureChain ---------------------------------------------------------

    public List<String> featureChain() {
        return featureChain;
    }

    // -- receiverExpression ---------------------------------------------------

    /**
     * The synthesized parent-navigation receiver expression — the root symbol
     * reference wrapped in an {@code RFeatureCall} for each feature BEFORE the leaf
     * (i.e. {@code root} + {@code featureChain[:-1]}). Built by
     * {@code AstBuilder.buildOnlyExistsElement} for a named-root element with a
     * non-empty feature chain so the existing symbol-linker + type-directed resolver
     * populate {@code resolvedSymbol}/{@code resolvedFeature} on it, giving the
     * generator the resolved parent type to render {@code only exists} like upstream
     * 9.83.0. Since v3.2 seat 8 it is the RECEIVER of {@link #leafReference()} — the
     * same node OBJECT, reached through the leaf, so every consumer that read the node
     * reads the same node; its RANGE is not the same on a two-or-more-feature path: it
     * ends at its own feature token now, where the pre-seat receiver spanned the whole
     * element (on a one-feature path it is the bare root under both). The validator's
     * only-exists diagnostics read that range as an anchor, and the released plugin was
     * MEASURED at round 2: the parent-path diagnostics (the cardinality warning, the
     * parent-path equality error) anchor on exactly this receiver's range, the element
     * diagnostics on the whole element — {@code ExpressionValidator.parentRangeOf} /
     * {@code rangeOf}, pinned by {@code OnlyExistsDiagnosticAnchorTest}.
     *
     * <p>{@code null} for the one edge shape the generator still declines on (a root
     * with no feature chain — a bare symbol or a bare {@code item} — whose parent is the
     * implicit variable itself), which keeps the legacy placeholder rendering. Since v3.2
     * seat 12 (D52, H3) an {@code item} root synthesizes its path like a named one: the
     * root node is a real, non-synthetic {@code RImplicitVariable} (the user wrote
     * {@code item}), so a one-hop {@code item -> p} carries that variable here and a
     * longer path the hop before its leaf — the generator's gm-aware receiver walk types
     * the implicit variable from its context (a lambda item, a rule input, a condition's
     * declaring type).
     */
    public RExpression receiverExpression() {
        return receiverExpression;
    }

    public void setReceiverExpression(RExpression receiverExpression) {
        checkMutable();
        this.receiverExpression = receiverExpression;
    }

    // -- leafReference (v3.2 seat 8, F15) -------------------------------------

    /**
     * The whole written path as ONE expression — {@code root -> chain[0] -> ... ->
     * chain[n-1]}, the LEAF included — whose receiver is {@link #receiverExpression()}.
     * Upstream models an {@code only exists} argument as an ordinary
     * {@code RosettaFeatureCall} and binds its leaf like any other feature; until
     * seat 8 the fork's leaf was a bare string in {@link #featureChain()}, so the
     * resolver never bound it and the resolution differential had no record to emit
     * at its offset (the chaos cell's 36 F15 rows). The leaf is a real
     * {@link RFeatureCall} now, ranged from the element's start to the leaf token,
     * resolved by the same passes as every navigation, and the node the
     * {@code children()} walk enters.
     *
     * <p>{@code null} exactly where {@link #receiverExpression()} is (the declined
     * edge shapes).
     */
    public RFeatureCall leafReference() {
        return leafReference;
    }

    public void setLeafReference(RFeatureCall leafReference) {
        checkMutable();
        this.leafReference = leafReference;
    }

    // -- the enclosure test (ONE declaration, LAW 69) --------------------------

    /**
     * True when {@code node} sits anywhere INSIDE an only-exists element — under the
     * synthesized path ({@link #leafReference()} / {@link #receiverExpression()}) or
     * a render-time twin parented into it. THE ONE read every consumer that exempts
     * only-exists paths consults (the resolver's diagnostic exemption, the IR
     * adapter's decode, the optimised route's chain classifier and alias-seam policy).
     *
     * <p>Why an ancestor walk and not a parent test: before v3.2 seat 8 the receiver
     * chain was the element's direct child, and the optimised route's classifier
     * exempted "a chain whose PARENT is the element"; the leaf node put an
     * {@link RFeatureCall} between them, the generator's synthesized item-rooted
     * receiver twin (parented where the root reference sits) stopped matching, and
     * eight cdm 5.38.0 only-exists receivers became ladder candidates — caught by the
     * optimised suite's conversion-event pin (782 → 790 on {@code cdm/5.38.0}, the FIRST
     * pin to fail; re-taken with a preserved print at the round-1 head with the
     * classifier's read reverted to the parent test by text —
     * {@code target/v32-seat8-instruments/scratch/plus8-r1.log}, local — which also
     * measured the class's reach: 94 such twins over 20 of the 21 function-bearing
     * cells, every cell but chaos, beside the 34 root twins of the two-or-more-arrow
     * paths; {@code sites-diff-B-G1.txt}) before any emission shipped. A structural
     * exemption must read the
     * STRUCTURE, however deep the path grows; the walk is bounded by
     * {@link #ENCLOSURE_WALK_LIMIT} like every ancestor walk of the IR adapter.
     */
    public static boolean encloses(RNode node) {
        int depth = 0;
        for (RNode up = node == null ? null : node.parent();
                up != null && depth++ < ENCLOSURE_WALK_LIMIT; up = up.parent()) {
            if (up instanceof ROnlyExistsElement) {
                return true;
            }
        }
        return false;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Exposes the synthesized path so every {@code children()}-based pass —
     * parent-linking, symbol resolution, type inference, type-directed feature
     * resolution, validation, code-gen — reaches it automatically: the
     * {@link #leafReference()} (whose subtree carries the {@link #receiverExpression()})
     * when the leaf was synthesized, the bare receiver otherwise, an empty list when
     * nothing was synthesized (the declined edge shapes).
     */
    @Override
    public List<? extends RNode> children() {
        if (leafReference != null) {
            return List.of(leafReference);
        }
        return receiverExpression != null ? List.of(receiverExpression) : List.of();
    }
}
