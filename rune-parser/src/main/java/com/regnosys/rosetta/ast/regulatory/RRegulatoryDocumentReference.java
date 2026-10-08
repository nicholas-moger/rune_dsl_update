package com.regnosys.rosetta.ast.regulatory;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Regulatory document reference node, corresponding to the
 * {@code regulatoryDocumentReference} grammar rule.
 *
 * <p>Groups a body reference with one or more corpus references and optional
 * segment references, such as
 * {@code CFTC MiFIR_RTS article "13" section "2"}.
 *
 * <p>Grammar:
 * <pre>
 * regulatoryDocumentReference:
 *     body=[RosettaBody|qualifiedName]
 *     corpuses+=[RosettaCorpus|qualifiedName]+
 *     segments+=rosettaSegmentRef*
 * ;
 * </pre>
 */
public class RRegulatoryDocumentReference extends RNode {

    private String bodyRef;
    private final List<String> corpusRefs = new ArrayList<>();
    private final List<RSegmentRef> segmentRefs = new ArrayList<>();

    // -- bodyRef --------------------------------------------------------------

    /**
     * Returns the body reference (first qualifiedName).
     */
    public String bodyRef() {
        return bodyRef;
    }

    public void setBodyRef(String bodyRef) {
        checkMutable();
        this.bodyRef = bodyRef;
    }

    // -- corpusRefs -----------------------------------------------------------

    /**
     * Returns the list of corpus references (one or more qualifiedNames).
     */
    public List<String> corpusRefs() {
        return corpusRefs;
    }

    // -- segmentRefs ----------------------------------------------------------

    /**
     * Returns the list of segment references.
     */
    public List<RSegmentRef> segmentRefs() {
        return segmentRefs;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable view of the segment references, per the
     * {@link RNode#children()} contract.
     */
    @Override
    public List<? extends RNode> children() {
        return List.copyOf(segmentRefs);
    }
}
