package com.regnosys.rosetta.ast.regulatory;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Document reference node, corresponding to the {@code docReference}
 * grammar rule.
 *
 * <p>Represents either a {@code [regulatoryReference ...]} or
 * {@code [docReference ...]} block attached to data types, enums,
 * attributes, functions, rules, conditions, and other elements.
 *
 * <p>Grammar:
 * <pre>
 * docReference:
 *     LSQUARE (REGULATORY_REFERENCE | DOC_REFERENCE)
 *     (FOR annotationPathExpression)?
 *     regulatoryDocumentReference
 *     regulatoryReferenceArgs?                  // P1.4.2 H4 — see U010
 *     documentRationale*
 *     (STRUCTURED_PROVISION structuredProvision=STRING)?
 *     (PROVISION provision=STRING)?
 *     REPORTED_FIELD?
 *     RSQUARE
 * ;
 * </pre>
 */
public class RDocReference extends RNode {

    private boolean regulatoryReference;
    private RAnnotationPathExpression forPath;
    private RRegulatoryDocumentReference regulatoryDocRef;
    private final List<RDocumentRationale> rationales = new ArrayList<>();
    private String structuredProvision;
    private String provision;
    private boolean reportedField;

    // -- regulatoryReference --------------------------------------------------

    /**
     * Returns {@code true} if this is a {@code REGULATORY_REFERENCE},
     * {@code false} if it is a {@code DOC_REFERENCE}.
     */
    public boolean isRegulatoryReference() {
        return regulatoryReference;
    }

    public void setRegulatoryReference(boolean regulatoryReference) {
        checkMutable();
        this.regulatoryReference = regulatoryReference;
    }

    // -- forPath --------------------------------------------------------------

    /**
     * Returns the optional {@code FOR} annotation path expression.
     */
    public Optional<RAnnotationPathExpression> forPath() {
        return Optional.ofNullable(forPath);
    }

    public void setForPath(RAnnotationPathExpression forPath) {
        checkMutable();
        this.forPath = forPath;
    }

    // -- regulatoryDocRef -----------------------------------------------------

    /**
     * Returns the regulatory document reference (body + corpus + segments).
     */
    public RRegulatoryDocumentReference regulatoryDocRef() {
        return regulatoryDocRef;
    }

    public void setRegulatoryDocRef(RRegulatoryDocumentReference regulatoryDocRef) {
        checkMutable();
        this.regulatoryDocRef = regulatoryDocRef;
    }

    // -- rationales -----------------------------------------------------------

    /**
     * Returns the list of document rationales.
     */
    public List<RDocumentRationale> rationales() {
        return rationales;
    }

    // -- structuredProvision --------------------------------------------------

    /**
     * Returns the optional structured provision text.
     */
    public Optional<String> structuredProvision() {
        return Optional.ofNullable(structuredProvision);
    }

    public void setStructuredProvision(String structuredProvision) {
        checkMutable();
        this.structuredProvision = structuredProvision;
    }

    // -- provision ------------------------------------------------------------

    /**
     * Returns the optional provision text.
     */
    public Optional<String> provision() {
        return Optional.ofNullable(provision);
    }

    public void setProvision(String provision) {
        checkMutable();
        this.provision = provision;
    }

    // -- reportedField --------------------------------------------------------

    /**
     * Returns {@code true} if the {@code REPORTED_FIELD} keyword is present.
     */
    public boolean isReportedField() {
        return reportedField;
    }

    public void setReportedField(boolean reportedField) {
        checkMutable();
        this.reportedField = reportedField;
    }

    // -- namedArgs (P1.4.2 H4) ------------------------------------------------

    private final List<RRegulatoryReferenceArg> namedArgs = new ArrayList<>();

    public List<RRegulatoryReferenceArg> namedArgs() {
        return namedArgs;
    }

    public void addNamedArg(RRegulatoryReferenceArg arg) {
        checkMutable();
        namedArgs.add(arg);
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (forPath != null) {
            result.add(forPath);
        }
        if (regulatoryDocRef != null) {
            result.add(regulatoryDocRef);
        }
        result.addAll(namedArgs);            // P1.4.2 H4
        result.addAll(rationales);
        return List.copyOf(result);
    }
}
