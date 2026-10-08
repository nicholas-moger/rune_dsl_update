package com.regnosys.rosetta.ast.annotations;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Annotation path expression node, corresponding to the
 * {@code annotationPathExpression} grammar rule.
 *
 * <p>Represents a path such as {@code item -> partyRole ->> role} used in
 * label annotations and rule reference annotations. The {@code root} is
 * either a {@code validID} or the {@code ITEM} keyword. Deep navigation
 * uses the {@code DEEP_ARROW} ({@code ->>}) token, distinct from the
 * shallow {@code ARROW} ({@code ->}) token.
 *
 * <p>Grammar:
 * <pre>
 * annotationPathExpression:
 *     (validID | ITEM) (ARROW validID | DEEP_ARROW validID)*
 * ;
 * </pre>
 */
public class RAnnotationPathExpression extends RNode {

    private String root;
    private boolean rootIsItem;
    private final List<RAnnotationPathSegment> segments = new ArrayList<>();

    // -- root -----------------------------------------------------------------

    public String root() {
        return root;
    }

    public void setRoot(String root) {
        checkMutable();
        this.root = root;
    }

    // -- rootIsItem -----------------------------------------------------------

    /**
     * Returns {@code true} if the root of this path expression is the
     * {@code ITEM} keyword rather than a named attribute.
     */
    public boolean isRootItem() {
        return rootIsItem;
    }

    public void setRootIsItem(boolean rootIsItem) {
        checkMutable();
        this.rootIsItem = rootIsItem;
    }

    // -- segments -------------------------------------------------------------

    public List<RAnnotationPathSegment> segments() {
        return segments;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable view of the path segments, per the
     * {@link RNode#children()} contract.
     */
    @Override
    public List<? extends RNode> children() {
        return List.copyOf(segments);
    }
}
