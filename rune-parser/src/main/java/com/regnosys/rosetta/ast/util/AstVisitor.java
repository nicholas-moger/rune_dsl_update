package com.regnosys.rosetta.ast.util;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;

import java.util.Objects;

/**
 * Visitor for the typed Rosetta AST.
 *
 * <p>Provides a generic dispatch entry point ({@link #visit(RNode)}) plus
 * coarse-grained category methods that callers can override to handle entire
 * node families with a single method:
 *
 * <ul>
 *   <li>{@link #visitRootElement(RRootElement)} — top-level declarations
 *       (data types, functions, enumerations, choices, type aliases, basic
 *       types, record types, meta types, library functions, rules, reports,
 *       bodies, corpora, segments, annotations, synonym sources, external
 *       sources)</li>
 *   <li>{@link #visitBinaryExpression(RBinaryExpression)} — binary expression
 *       forms (arithmetic, comparison, equality, logical, contains, disjoint,
 *       default, join)</li>
 *   <li>{@link #visitExpression(RExpression)} — any expression that is not a
 *       binary expression (literals, references, postfix operations,
 *       conditionals, constructors, etc.)</li>
 *   <li>{@link #visitNode(RNode)} — any node that is not an expression or
 *       root element (supporting types, sub-structures, mapping forms, etc.)</li>
 *   <li>{@link #visitDefault(RNode)} — fallback called by all of the above
 *       when not overridden; default returns {@code null}</li>
 * </ul>
 *
 * <p>For finer-grained dispatch, callers should use Java 21 pattern-matching
 * switch directly inside their visitor implementation:
 *
 * <pre>
 *   AstVisitor&lt;String&gt; namer = new AstVisitor&lt;&gt;() {
 *       &#64;Override public String visit(RNode node) {
 *           return switch (node) {
 *               case RDataType dt -&gt; "type " + dt.name();
 *               case RFunction fn -&gt; "func " + fn.name();
 *               case REnumeration en -&gt; "enum " + en.name();
 *               default -&gt; null;
 *           };
 *       }
 *   };
 * </pre>
 *
 * <p>This approach is preferred over a 129-method visitor interface because:
 * <ul>
 *   <li>Pattern-matching switch gives compile-time exhaustiveness when used
 *       with sealed types (and the AST hierarchies can be sealed in M3+).</li>
 *   <li>It avoids forcing every consumer to implement or override dozens of
 *       irrelevant methods.</li>
 *   <li>It puts node-type dispatch logic next to the consumer's handling
 *       code rather than spreading it across a separate interface.</li>
 * </ul>
 *
 * <p>For pure side-effect traversal without a return value, use
 * {@link AstWalker#walk(RNode, java.util.function.Consumer)} instead — the
 * {@code AstVisitor} interface is intended for transformations that produce
 * a value per node (e.g., string rendering, code generation, type checking).
 *
 * @param <T> the type produced by visiting a node
 */
public interface AstVisitor<T> {

    /**
     * Generic entry point — dispatches to the appropriate category method
     * based on the node's runtime type.
     *
     * <p>Default implementation routes:
     * <ul>
     *   <li>{@link RBinaryExpression} → {@link #visitBinaryExpression}</li>
     *   <li>{@link RExpression} (non-binary) → {@link #visitExpression}</li>
     *   <li>{@link RRootElement} → {@link #visitRootElement}</li>
     *   <li>everything else → {@link #visitNode}</li>
     * </ul>
     *
     * <p>Implementations may override this method directly to do custom
     * dispatch (e.g., a pattern-matching switch on concrete types).
     *
     * @param node the node to visit; must not be null
     * @return the result of visiting the node
     */
    default T visit(RNode node) {
        return switch (node) {
            case RBinaryExpression bin -> visitBinaryExpression(bin);
            case RExpression expr -> visitExpression(expr);
            case RRootElement root -> visitRootElement(root);
            default -> visitNode(node);
        };
    }

    /**
     * Returns a visitor that consults the given {@link AstInterfaceRegistry}
     * first on {@link #visit(RNode)}, falling back to this visitor when the
     * registry has no matching handler. The wrapper is a full decorator —
     * every category method ({@link #visitBinaryExpression},
     * {@link #visitExpression}, {@link #visitRootElement}, {@link #visitNode},
     * {@link #visitDefault}) delegates to the base visitor so callers that
     * invoke a category method directly still see the base visitor's
     * behaviour. Only {@link #visit(RNode)} is intercepted to consult the
     * registry first.
     *
     * <p>Per H9 audit hook + U004 manifest entry. See
     * {@code docs/upgrades/U004-ast-interface-registry.md}.
     */
    default AstVisitor<T> withInterfaceRegistry(AstInterfaceRegistry<T> registry) {
        Objects.requireNonNull(registry, "registry must not be null");
        AstVisitor<T> base = this;
        return new AstVisitor<T>() {
            @Override public T visit(RNode node) {
                T fromRegistry = registry.dispatch(node);
                return fromRegistry != null ? fromRegistry : base.visit(node);
            }
            @Override public T visitBinaryExpression(RBinaryExpression node) {
                return base.visitBinaryExpression(node);
            }
            @Override public T visitExpression(RExpression node) {
                return base.visitExpression(node);
            }
            @Override public T visitRootElement(RRootElement node) {
                return base.visitRootElement(node);
            }
            @Override public T visitNode(RNode node) {
                return base.visitNode(node);
            }
            @Override public T visitDefault(RNode node) {
                return base.visitDefault(node);
            }
        };
    }

    /**
     * Called for any node that is a {@link RBinaryExpression} (arithmetic,
     * comparison, equality, logical, contains, disjoint, default, join).
     * Default implementation delegates to {@link #visitDefault}.
     */
    default T visitBinaryExpression(RBinaryExpression node) {
        return visitDefault(node);
    }

    /**
     * Called for any expression that is not a {@link RBinaryExpression}
     * (literals, references, postfix operations, conditionals, constructors,
     * etc.). Default implementation delegates to {@link #visitDefault}.
     */
    default T visitExpression(RExpression node) {
        return visitDefault(node);
    }

    /**
     * Called for any top-level declaration (data types, functions,
     * enumerations, choices, etc.). Default implementation delegates to
     * {@link #visitDefault}.
     */
    default T visitRootElement(RRootElement node) {
        return visitDefault(node);
    }

    /**
     * Called for any node that is neither an expression nor a root element
     * (supporting types, sub-structures, mapping forms, etc.). Default
     * implementation delegates to {@link #visitDefault}.
     */
    default T visitNode(RNode node) {
        return visitDefault(node);
    }

    /**
     * Fallback called by all category methods when not overridden. Default
     * returns {@code null}. Override this to provide a single common
     * behaviour for all unhandled node types.
     */
    default T visitDefault(RNode node) {
        return null;
    }
}
