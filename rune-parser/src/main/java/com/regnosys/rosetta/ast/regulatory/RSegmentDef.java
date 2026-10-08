package com.regnosys.rosetta.ast.regulatory;

import com.regnosys.rosetta.ast.RRootElement;

/**
 * Segment definition node, corresponding to the {@code rosettaSegment}
 * grammar rule.
 *
 * <p>Represents a segment declaration such as {@code segment article}
 * or one of the keyword-based segments ({@code RATIONALE},
 * {@code RATIONALE_AUTHOR}, {@code STRUCTURED_PROVISION}).
 *
 * <p>Note: Segment definitions are NOT {@code RDefinable} — they have
 * no optional definition text.
 *
 * <p>Grammar:
 * <pre>
 * rosettaSegment:
 *     SEGMENT (validID | RATIONALE | RATIONALE_AUTHOR | STRUCTURED_PROVISION)
 * ;
 * </pre>
 */
public class RSegmentDef extends RRootElement {

    private String name;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }
}
