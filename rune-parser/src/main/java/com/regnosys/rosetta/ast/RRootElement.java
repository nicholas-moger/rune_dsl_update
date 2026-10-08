package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.annotations.RRuneAnnotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all top-level (root) declarations in a Rosetta model.
 *
 * <p>Root elements are the children of the {@code rootElement} rule in the grammar:
 * data types, enums, choices, functions, annotations, rules, reports,
 * type aliases, synonym sources, bodies, corpora, segments, built-in types,
 * record types, library functions, meta types, and external sources.
 *
 * <p>Subclasses include concrete node types for each of these grammar alternatives.
 *
 * <p><strong>Rune annotations slot (P1.4.2 H2 / H11):</strong> hoisted here as a
 * single source of truth so all root elements have a uniform AST surface for
 * {@code @-prefix} annotations. Only the 5 audit-listed attach sites
 * ({@link com.regnosys.rosetta.ast.types.RDataType},
 * {@link com.regnosys.rosetta.ast.types.RChoice},
 * {@link com.regnosys.rosetta.ast.types.REnumeration},
 * {@link com.regnosys.rosetta.ast.functions.RFunction},
 * {@link com.regnosys.rosetta.ast.functions.RRule}) parse the {@code runeAnnotations?}
 * grammar prefix in this PR; other root elements inherit the empty list as a reserved
 * surface for future grammar additions.
 */
public abstract class RRootElement extends RNode {

    private final List<RRuneAnnotation> runeAnnotations = new ArrayList<>();

    public List<RRuneAnnotation> runeAnnotations() {
        return runeAnnotations;
    }

    public void addRuneAnnotation(RRuneAnnotation a) {
        checkMutable();
        runeAnnotations.add(a);
    }

    /**
     * Default-includes hoisted rune annotations in the child traversal.
     * Subclasses that override {@link #children()} should call {@code super.children()}
     * and concatenate to surface the hoisted annotations alongside their own children.
     */
    @Override
    public List<? extends RNode> children() {
        return List.copyOf(runeAnnotations);
    }
}
