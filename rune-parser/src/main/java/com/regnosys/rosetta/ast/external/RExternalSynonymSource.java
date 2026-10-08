package com.regnosys.rosetta.ast.external;

import com.regnosys.rosetta.ast.RNode;

import com.regnosys.rosetta.ast.synonyms.RSynonymSource;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.ArrayList;
import java.util.List;

/**
 * External synonym source declaration node, corresponding to the
 * {@code rosettaExternalSynonymSource} grammar rule.
 *
 * <p>Represents an external synonym source that maps external schema elements
 * to Rosetta model types. This is a top-level (root) element.
 *
 * <p>Extends {@link RSynonymSource}, mirroring upstream's Ecore hierarchy
 * exactly ({@code RosettaExternalSynonymSource extends ExternalAnnotationSource,
 * RosettaSynonymSource} — Rosetta.xcore:480): the {@code extends} cross-reference
 * on an external synonym source is typed {@code [RosettaSynonymSource|QualifiedName]}
 * in upstream's grammar, so the plain legacy form and the external form are BOTH
 * admissible super-source targets (PR #450; corpus witness:
 * {@code synonym source FIS extends FIS_BASE} where {@code FIS_BASE} is the
 * plain body-less form declared two lines above in the same file).
 *
 * <p>Grammar:
 * <pre>
 * rosettaExternalSynonymSource:
 *     SYNONYM SOURCE qualifiedName
 *     (EXTENDS qualifiedName (COMMA qualifiedName)*)?
 *     LBRACE
 *         rosettaExternalClass*
 *         (ENUMS rosettaExternalEnum*)?
 *     RBRACE
 * ;
 * </pre>
 */
public class RExternalSynonymSource extends RSynonymSource {

    private final List<String> superSourceNames = new ArrayList<>();
    private final List<RExternalClass> classes = new ArrayList<>();
    private final List<RExternalEnum> enums = new ArrayList<>();

    // -- superSourceNames -----------------------------------------------------

    public List<String> superSourceNames() {
        return superSourceNames;
    }

    // -- classes --------------------------------------------------------------

    public List<RExternalClass> classes() {
        return classes;
    }

    // -- enums ----------------------------------------------------------------

    public List<RExternalEnum> enums() {
        return enums;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        result.addAll(classes);
        result.addAll(enums);
        return List.copyOf(result);
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.EXTERNAL_SOURCE_NOT_FOUND, tokenRangePrefix = "superSource")
    private final java.util.List<RSynonymSource> resolvedSuperSources = new java.util.ArrayList<>();

    public java.util.List<RSynonymSource> superSources() { return java.util.List.copyOf(resolvedSuperSources); }
    public void addResolvedSuperSource(RSynonymSource source) { checkMutable(); this.resolvedSuperSources.add(source); }
}
