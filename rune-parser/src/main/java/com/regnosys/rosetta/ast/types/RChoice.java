package com.regnosys.rosetta.ast.types;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.synonyms.RClassSynonym;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Choice type declaration node, corresponding to the {@code choice} grammar rule.
 *
 * <p>Represents a {@code choice Foo:} declaration with options.
 * NOTE: The grammar does NOT include {@code extends} for choice types.
 *
 * <p>Grammar:
 * <pre>
 * choice:
 *     runeAnnotations?                          // P1.4.2 H2 — hoisted from RRootElement, see U008
 *     CHOICE qualifiedName definable?
 *     annotationRef*
 *     classSynonym*
 *     options+=choiceOption (COMMA options+=choiceOption)*
 * ;
 * </pre>
 */
public class RChoice extends RRootElement implements RDefinable {

    private String name;
    private String definition;
    private final List<RChoiceOption> options = new ArrayList<>();

    private final List<RAnnotationRef> annotationRefs = new ArrayList<>();

    private final List<RClassSynonym> classSynonyms = new ArrayList<>();

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- definition (RDefinable) ----------------------------------------------

    @Override
    public Optional<String> definition() {
        return Optional.ofNullable(definition);
    }

    public void setDefinition(String definition) {
        checkMutable();
        this.definition = definition;
    }

    // -- options --------------------------------------------------------------

    public List<RChoiceOption> options() {
        return options;
    }

    // -- annotationRefs -------------------------------------------------------

    public List<RAnnotationRef> annotationRefs() {
        return annotationRefs;
    }

    // -- classSynonyms --------------------------------------------------------

    public List<RClassSynonym> classSynonyms() {
        return classSynonyms;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        result.addAll(annotationRefs);
        result.addAll(classSynonyms);
        result.addAll(options);
        return List.copyOf(result);
    }
}
