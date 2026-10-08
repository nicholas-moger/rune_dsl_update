package com.regnosys.rosetta.ast.annotations;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Label annotation node, corresponding to the {@code labelAnnotation}
 * grammar rule.
 *
 * <p>Represents a label annotation such as
 * {@code [label for trade -> partyRole as "Party Role"]}.
 *
 * <p>Grammar:
 * <pre>
 * labelAnnotation:
 *     LBRACK LABEL (FOR annotationPathExpression)?
 *     (annotationPathExpression? AS)? STRING RBRACK
 * ;
 * </pre>
 */
public class RLabelAnnotation extends RNode {

    private RAnnotationPathExpression forPath;
    private RAnnotationPathExpression asPath;
    private String label;

    // -- forPath --------------------------------------------------------------

    public Optional<RAnnotationPathExpression> forPath() {
        return Optional.ofNullable(forPath);
    }

    public void setForPath(RAnnotationPathExpression forPath) {
        checkMutable();
        this.forPath = forPath;
    }

    // -- asPath ---------------------------------------------------------------

    public Optional<RAnnotationPathExpression> asPath() {
        return Optional.ofNullable(asPath);
    }

    public void setAsPath(RAnnotationPathExpression asPath) {
        checkMutable();
        this.asPath = asPath;
    }

    // -- label ----------------------------------------------------------------

    public String label() {
        return label;
    }

    public void setLabel(String label) {
        checkMutable();
        this.label = label;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (forPath != null) {
            result.add(forPath);
        }
        if (asPath != null) {
            result.add(asPath);
        }
        return List.copyOf(result);
    }
}
