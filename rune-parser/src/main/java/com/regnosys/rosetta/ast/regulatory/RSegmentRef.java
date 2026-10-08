package com.regnosys.rosetta.ast.regulatory;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

/**
 * Segment reference node, corresponding to the {@code rosettaSegmentRef}
 * grammar rule.
 *
 * <p>Represents a reference to a segment with a string value, such as
 * {@code article "13"} or {@code section "2"}.
 *
 * <p>Grammar:
 * <pre>
 * rosettaSegmentRef:
 *     segment=[RosettaSegment|qualifiedName] segmentValue=STRING
 * ;
 * </pre>
 */
public class RSegmentRef extends RNode {

    private String segmentName;
    private String value;

    // -- segmentName ----------------------------------------------------------

    /**
     * Returns the segment name (qualifiedName reference).
     */
    public String segmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        checkMutable();
        this.segmentName = segmentName;
    }

    // -- value ----------------------------------------------------------------

    /**
     * Returns the segment value (STRING literal).
     */
    public String value() {
        return value;
    }

    public void setValue(String value) {
        checkMutable();
        this.value = value;
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.SEGMENT_DEFINITION_NOT_FOUND)
    private com.regnosys.rosetta.ast.regulatory.RSegmentDef resolvedSegment;

    public java.util.Optional<com.regnosys.rosetta.ast.regulatory.RSegmentDef> segment() { return java.util.Optional.ofNullable(resolvedSegment); }
    public void setResolvedSegment(com.regnosys.rosetta.ast.regulatory.RSegmentDef resolved) { checkMutable(); this.resolvedSegment = resolved; }
}
