package com.regnosys.rosetta.ast.regulatory;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.enums.ReportTiming;
import com.regnosys.rosetta.ast.external.RExternalRuleSource;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Report declaration node, corresponding to the {@code rosettaReport}
 * grammar rule.
 *
 * <p>Represents a report declaration such as:
 * <pre>
 * report CFTC MiFIR_RTS in T+1
 *     from TradeInstruction
 *     when IsReportable and IsCleared
 *     using standard ISDA_Create
 *     with type MiFIRReport
 *     with source MiFIRSource
 * </pre>
 *
 * <p>Note: Reports are NOT {@code RDefinable} and have NO name field.
 *
 * <p>Grammar:
 * <pre>
 * rosettaReport:
 *     REPORT regulatoryDocumentReference
 *     IN timing
 *     FROM typeCall
 *     WHEN qualifiedName (AND qualifiedName)*
 *     (USING STANDARD qualifiedName)?
 *     WITH TYPE qualifiedName
 *     (WITH SOURCE qualifiedName)?
 * ;
 * </pre>
 */
public class RReport extends RRootElement {

    private RRegulatoryDocumentReference regulatoryDocRef;
    private ReportTiming timing;
    private RTypeCall fromType;
    private final List<String> whenConditions = new ArrayList<>();
    private String usingStandard;
    private String withType;
    private String withSource;

    // === M3 resolved fields (v3.2 seat 4, PR #625 — F7) ======================
    /**
     * The linker-resolved {@code with type} target — the report's output type BY ID, resolved in
     * {@code GlobalResolutionPass.resolveReport} like every other reference (own namespace, then
     * imports; upstream's {@code reportType=[Data|QualifiedName]}). Before this field the generator
     * typed the synthetic output by NAME and a workspace-wide first match handed the chaos s07
     * report functions the FIRST namespace's report type and rules (eleven D11 rows, and the
     * emissions did not compile).
     */
    @CrossRefField(category = DiagnosticCategory.TYPE_NOT_FOUND, tokenRangeKey = "withType")
    private SymbolId withTypeId;
    /** The linker-resolved {@code with source} target — the same law for the rule source. */
    @CrossRefField(category = DiagnosticCategory.EXTERNAL_SOURCE_NOT_FOUND, tokenRangeKey = "withSource")
    private SymbolId withSourceId;

    // -- regulatoryDocRef -----------------------------------------------------

    /**
     * Returns the regulatory document reference for this report.
     */
    public RRegulatoryDocumentReference regulatoryDocRef() {
        return regulatoryDocRef;
    }

    public void setRegulatoryDocRef(RRegulatoryDocumentReference regulatoryDocRef) {
        checkMutable();
        this.regulatoryDocRef = regulatoryDocRef;
    }

    // -- timing ---------------------------------------------------------------

    /**
     * Returns the report timing (e.g., {@code T_PLUS_1}, {@code REAL_TIME}).
     */
    public ReportTiming timing() {
        return timing;
    }

    public void setTiming(ReportTiming timing) {
        checkMutable();
        this.timing = timing;
    }

    // -- fromType -------------------------------------------------------------

    /**
     * Returns the {@code FROM typeCall} source type.
     */
    public RTypeCall fromType() {
        return fromType;
    }

    public void setFromType(RTypeCall fromType) {
        checkMutable();
        this.fromType = fromType;
    }

    // -- whenConditions -------------------------------------------------------

    /**
     * Returns the list of {@code WHEN} condition names (qualifiedNames).
     */
    public List<String> whenConditions() {
        return whenConditions;
    }

    // -- usingStandard --------------------------------------------------------

    /**
     * Returns the optional {@code USING STANDARD} reference.
     */
    public Optional<String> usingStandard() {
        return Optional.ofNullable(usingStandard);
    }

    public void setUsingStandard(String usingStandard) {
        checkMutable();
        this.usingStandard = usingStandard;
    }

    // -- withType -------------------------------------------------------------

    /**
     * Returns the {@code WITH TYPE} reference (qualifiedName).
     */
    public String withType() {
        return withType;
    }

    public void setWithType(String withType) {
        checkMutable();
        this.withType = withType;
    }

    // -- withSource -----------------------------------------------------------

    /**
     * Returns the optional {@code WITH SOURCE} reference.
     */
    public Optional<String> withSource() {
        return Optional.ofNullable(withSource);
    }

    public void setWithSource(String withSource) {
        checkMutable();
        this.withSource = withSource;
    }

    // -- withTypeId / withSourceId (linker-resolved; v3.2 seat 4). The ids are READ by the generator through
    // its workspace (RuleReferenceTraversal) and by the synthetic output RFunction.fromReport types from them;
    // round 1 deleted two never-called resolving accessors (their empty-when-unresolved contract was false on
    // a detached node, where workspace() throws) and hoisted the resolver share onto RNode. --------------
    public Optional<SymbolId> withTypeId() {
        return Optional.ofNullable(withTypeId);
    }

    public void setWithTypeId(SymbolId id) {
        checkMutable();
        this.withTypeId = id;
    }

    public Optional<SymbolId> withSourceId() {
        return Optional.ofNullable(withSourceId);
    }

    public void setWithSourceId(SymbolId id) {
        checkMutable();
        this.withSourceId = id;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        if (regulatoryDocRef != null) {
            result.add(regulatoryDocRef);
        }
        if (fromType != null) {
            result.add(fromType);
        }
        return List.copyOf(result);
    }
}
