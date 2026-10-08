package com.regnosys.rosetta.ast.expressions.supporting;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Inline function node, corresponding to the {@code inlineFunction} and
 * {@code implicitInlineFunction} grammar rules.
 *
 * <p>Represents a lambda-like construct in expressions. An inline function
 * has zero or more named parameters and a body expression. When the function
 * comes from an {@code implicitInlineFunction} (no explicit parameter list),
 * {@link #isImplicit()} returns {@code true}.
 *
 * <p>Grammar:
 * <pre>
 * inlineFunction:
 *     LBRACKET (closureParameter (COMMA closureParameter)*)? RBRACKET expression
 * ;
 * implicitInlineFunction:
 *     expression
 * ;
 * </pre>
 *
 * <p><b>Two views of the parameters, kept in lockstep at construction</b> (v3.2
 * seat 8). {@link #parameters()} holds the DECLARED parameters as nodes —
 * upstream's {@code ClosureParameter}s, the binding targets a reference to
 * {@code x} resolves to. {@link #paramNames()} holds the NAMES the lexical scope
 * sees: every declared parameter's name plus, for an implicit lambda, the
 * synthetic {@code item} the derived-state rule injects (upstream's implicit
 * variable, which has no node on either side). {@link #addParameter} is the ONE
 * writer of the node view ({@link #parameters()} is read-only) and appends the
 * name beside it; {@link #paramNames()} stays writable because the derived-state
 * rule and hand-built test lambdas add a NAME with no node — so the name view may
 * carry MORE names than the node view, never fewer. A name-only entry (the
 * synthetic {@code item}, or a lambda hand-built by a test through
 * {@code paramNames().add}) has no node, and a reference to it binds to this lambda
 * exactly as every closure parameter did before the node existed —
 * {@link #declaringLambdaOf} reads both shapes.
 */
public class RInlineFunction extends RNode {

    private final List<RClosureParameter> parameters = new ArrayList<>();
    private final List<String> paramNames = new ArrayList<>();
    private boolean implicit;
    private RExpression body;
    /** The {@link #children()} list, built once per (parameters, body) state — see {@link #children()}. */
    private List<RNode> childrenView;

    // -- parameters (the declared nodes) ---------------------------------------

    /**
     * The declared parameters in source order — upstream's {@code ClosureParameter}s.
     * An unmodifiable view: {@link #addParameter} is the only writer, so the node view
     * never carries a parameter the name view lacks.
     */
    public List<RClosureParameter> parameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Declares a parameter: the node joins {@link #parameters()} and its name
     * {@link #paramNames()} in the same call — the only way a node enters.
     */
    public void addParameter(RClosureParameter parameter) {
        checkMutable();
        parameters.add(parameter);
        paramNames.add(parameter.name());
        childrenView = null;
    }

    /** The declared parameter named {@code name}, if any (the synthetic {@code item} has none). */
    public Optional<RClosureParameter> parameter(String name) {
        for (RClosureParameter p : parameters) {
            if (p.name() != null && p.name().equals(name)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /**
     * THE ONE READ of a closure-parameter binding (LAW 69 — every consumer that
     * asks "is this resolved symbol the closure parameter {@code name}, and which
     * lambda declares it?" consults this): the declaring lambda when
     * {@code binding} is an {@link RClosureParameter} of that name, or when it is
     * an {@link RInlineFunction} whose {@link #paramNames()} carries the name (the
     * pre-node binding shape, still produced for a name-only parameter); empty
     * for every other node.
     */
    public static Optional<RInlineFunction> declaringLambdaOf(RNode binding, String name) {
        if (name == null || binding == null) {
            return Optional.empty();
        }
        if (binding instanceof RClosureParameter parameter) {
            return name.equals(parameter.name())
                    ? Optional.ofNullable(parameter.declaringFunction())
                    : Optional.empty();
        }
        if (binding instanceof RInlineFunction fn && fn.paramNames().contains(name)) {
            return Optional.of(fn);
        }
        return Optional.empty();
    }

    /**
     * True when {@code binding} is a closure-parameter binding TARGET in either shape — an
     * {@link RClosureParameter} node or an {@link RInlineFunction} (the name-only shape).
     * Name-agnostic: the caller supplies the name where it matters
     * ({@link #declaringLambdaOf}); this is the KIND test alone, and it answers true for
     * any inline function, one with no parameters included — safe at its call sites
     * because the inline scope only ever registers a lambda under one of its own names.
     */
    public static boolean isClosureParameterBinding(RNode binding) {
        return binding instanceof RClosureParameter || binding instanceof RInlineFunction;
    }

    // -- paramNames -----------------------------------------------------------

    public List<String> paramNames() {
        return paramNames;
    }

    // -- implicit -------------------------------------------------------------

    public boolean isImplicit() {
        return implicit;
    }

    public void setImplicit(boolean implicit) {
        checkMutable();
        this.implicit = implicit;
    }

    // -- body -----------------------------------------------------------------

    public RExpression body() {
        return body;
    }

    public void setBody(RExpression body) {
        checkMutable();
        this.body = body;
        childrenView = null;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * The declared parameters, then the body. The order is a convention, not a
     * contract any reader depends on: no reader indexes {@code children()}
     * positionally (measured at v3.2 seat 8 — the iterators are the parent wiring,
     * {@code freeze()} and the two walkers). Built once per (parameters, body) state
     * and re-used: {@code children()} is the hot path of every walk and of
     * {@code freeze()}, so the list is built once rather than per call (the round-1
     * code-quality NIT-5 — a design reason, not a measured saving).
     */
    @Override
    public List<? extends RNode> children() {
        List<RNode> view = childrenView;
        if (view == null) {
            if (parameters.isEmpty()) {
                view = body != null ? List.of(body) : List.of();
            } else {
                List<RNode> children = new ArrayList<>(parameters);
                if (body != null) {
                    children.add(body);
                }
                view = List.copyOf(children);
            }
            childrenView = view;
        }
        return view;
    }
}
