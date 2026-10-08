package com.regnosys.rosetta.ast.util;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Depth-first traversal utility for the typed AST.
 *
 * <p>Replaces the EMF {@code EcoreUtil2.eAllOfType} pattern used throughout
 * the legacy rune-dsl with a simple, type-safe walker. Used by validation
 * (M6), code generation (M7, M9), and IDE features (M8) to find AST nodes
 * matching a type or predicate.
 *
 * <p>All methods are static and stateless — safe to call concurrently from
 * multiple threads provided the underlying tree is not being mutated.
 * Traversal is pre-order (consumer/predicate sees a parent before its
 * children) and visits each reachable node exactly once via
 * {@link RNode#children()}. The walker is recursive, which is acceptable
 * for the AST tree depths produced by realistic Rune DSL files (CDM and
 * DRR top out around 30 levels). If pathologically deep trees ever become
 * a concern, the recursion can be converted to an iterative {@link java.util.ArrayDeque}-based walk
 * without changing the public API.
 *
 * <p>Example usage:
 * <pre>
 *   // Find every data type in a model
 *   List&lt;RDataType&gt; dataTypes = AstWalker.findAll(model, RDataType.class);
 *
 *   // Find the enclosing data type for an attribute
 *   Optional&lt;RDataType&gt; enclosing = AstWalker.findAncestor(attribute, RDataType.class);
 *
 *   // Print every node visited in pre-order
 *   AstWalker.walk(model, node -&gt; System.out.println(node.getClass().getSimpleName()));
 * </pre>
 */
public final class AstWalker {

    private AstWalker() {
        // utility class
    }

    /**
     * Walks the subtree rooted at {@code root} in depth-first pre-order,
     * invoking {@code consumer} for every node encountered.
     *
     * @param root     the root of the subtree to walk; must not be null
     * @param consumer the consumer invoked for each node; must not be null
     * @throws NullPointerException if {@code root} or {@code consumer} is null
     */
    public static void walk(RNode root, Consumer<RNode> consumer) {
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        walkInternal(root, consumer);
    }

    private static void walkInternal(RNode node, Consumer<RNode> consumer) {
        consumer.accept(node);
        for (RNode child : node.children()) {
            walkInternal(child, consumer);
        }
    }

    /**
     * Returns all nodes in the subtree rooted at {@code root} that are
     * instances of {@code type}, in depth-first pre-order.
     *
     * <p>This is the typed-AST equivalent of EMF's
     * {@code EcoreUtil2.eAllOfType(root, Type.class)}.
     *
     * @param root the root of the subtree to search
     * @param type the node type to collect
     * @param <T>  the node type
     * @return a list of matching nodes in pre-order; never null but may be empty
     * @throws NullPointerException if {@code root} or {@code type} is null
     */
    public static <T extends RNode> List<T> findAll(RNode root, Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        List<T> result = new ArrayList<>();
        walk(root, node -> {
            if (type.isInstance(node)) {
                result.add(type.cast(node));
            }
        });
        return result;
    }

    /**
     * Returns all nodes in the subtree rooted at {@code root} that match the
     * given predicate, in depth-first pre-order.
     *
     * @param root      the root of the subtree to search
     * @param predicate the predicate to test each node
     * @return a list of matching nodes in pre-order; never null but may be empty
     * @throws NullPointerException if any argument is null
     */
    public static List<RNode> findAll(RNode root, Predicate<RNode> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        List<RNode> result = new ArrayList<>();
        walk(root, node -> {
            if (predicate.test(node)) {
                result.add(node);
            }
        });
        return result;
    }

    /**
     * Returns the first ancestor of {@code node} (walking via
     * {@link RNode#parent()}) that is an instance of {@code type}, or empty
     * if no such ancestor exists.
     *
     * <p>The starting node itself is NOT considered — only strict ancestors.
     * To include the starting node, check {@code type.isInstance(node)}
     * separately.
     *
     * @param node the node to start from
     * @param type the ancestor type to find
     * @param <T>  the ancestor type
     * @return the nearest ancestor of the given type, or empty if none found
     * @throws NullPointerException if {@code node} or {@code type} is null
     */
    public static <T extends RNode> Optional<T> findAncestor(RNode node, Class<T> type) {
        Objects.requireNonNull(node, "node must not be null");
        Objects.requireNonNull(type, "type must not be null");
        RNode current = node.parent();
        while (current != null) {
            if (type.isInstance(current)) {
                return Optional.of(type.cast(current));
            }
            current = current.parent();
        }
        return Optional.empty();
    }

    /**
     * Returns the first node in the subtree rooted at {@code root} that is
     * an instance of {@code type}, or empty if none found. Stops the walk
     * as soon as a match is encountered.
     *
     * @param root the root of the subtree to search
     * @param type the node type to find
     * @param <T>  the node type
     * @return the first matching node in pre-order, or empty if none found
     * @throws NullPointerException if {@code root} or {@code type} is null
     */
    public static <T extends RNode> Optional<T> findFirst(RNode root, Class<T> type) {
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(type, "type must not be null");
        return findFirstInternal(root, type);
    }

    private static <T extends RNode> Optional<T> findFirstInternal(RNode node, Class<T> type) {
        if (type.isInstance(node)) {
            return Optional.of(type.cast(node));
        }
        for (RNode child : node.children()) {
            Optional<T> match = findFirstInternal(child, type);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    /**
     * Counts nodes in the subtree rooted at {@code root} that are instances
     * of {@code type}. Equivalent to {@code findAll(root, type).size()} but
     * avoids materialising the intermediate list — useful for structural
     * comparison and statistics gathering.
     *
     * @param root the root of the subtree to count
     * @param type the node type to count
     * @return the number of matching nodes
     * @throws NullPointerException if {@code root} or {@code type} is null
     */
    public static int count(RNode root, Class<? extends RNode> type) {
        Objects.requireNonNull(type, "type must not be null");
        int[] counter = {0};
        walk(root, node -> {
            if (type.isInstance(node)) {
                counter[0]++;
            }
        });
        return counter[0];
    }
}
