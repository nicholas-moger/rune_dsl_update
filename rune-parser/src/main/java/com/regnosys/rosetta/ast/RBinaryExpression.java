package com.regnosys.rosetta.ast;

import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for binary expressions that have both a left and right operand.
 *
 * <p>Covers grammar alternatives such as {@code AdditiveExpr},
 * {@code MultiplicativeExpr}, {@code ComparisonExpr}, {@code EqualityExpr},
 * {@code AndExpr}, {@code OrExpr}, {@code DefaultExpr}, {@code JoinExpr},
 * {@code ContainsExpr}, and {@code DisjointExpr}.
 *
 * <p>The {@link #children()} method returns left and right (filtering nulls),
 * which is critical for tree traversal.
 */
public abstract class RBinaryExpression extends RExpression {

    private RExpression left;
    private RExpression right;

    // -- Left -----------------------------------------------------------------

    /**
     * Returns the raw (nullable) left operand.
     *
     * <p>Named {@code rawLeft()} rather than {@code left()} because
     * {@link RExpression#left()} is already defined to return
     * {@code Optional<RExpression>}. Subclasses that override
     * {@code children()} use this to access the field directly.
     */
    public RExpression rawLeft() {
        return left;
    }

    public void setLeft(RExpression left) {
        checkMutable();
        this.left = left;
    }

    /**
     * Returns the left operand as an {@link Optional}, supporting the
     * expression-chain pattern where "without left" forms may have null.
     */
    @Override
    public Optional<RExpression> left() {
        return Optional.ofNullable(left);
    }

    // -- Right ----------------------------------------------------------------

    /**
     * Returns the raw (nullable) right operand.
     *
     * <p>Named {@code rawRight()} to mirror {@link #rawLeft()} and make the
     * nullable contract explicit. Subclasses that override {@code children()}
     * use this to access the field directly.
     */
    public RExpression rawRight() {
        return right;
    }

    /**
     * Returns the right operand as an {@link Optional}, mirroring
     * {@link #left()}. Use this in consumer code to avoid accidental null
     * dereferences; binary forms with no right operand (e.g., partial
     * "without right" trees built by M3 derived state) safely return empty.
     */
    public Optional<RExpression> right() {
        return Optional.ofNullable(right);
    }

    public void setRight(RExpression right) {
        checkMutable();
        this.right = right;
    }

    // -- Children (for traversal) ---------------------------------------------

    /**
     * Returns the left and right operands as child nodes, filtering nulls.
     * This ensures the tree walker can traverse binary expression subtrees.
     */
    @Override
    public List<? extends RNode> children() {
        if (left != null && right != null) return List.of(left, right);
        if (left != null) return List.of(left);
        if (right != null) return List.of(right);
        return List.of();
    }
}
