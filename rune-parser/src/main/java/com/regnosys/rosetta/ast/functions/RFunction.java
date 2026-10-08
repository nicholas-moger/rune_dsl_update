package com.regnosys.rosetta.ast.functions;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.enums.OperationOp;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.regulatory.RRegulatoryDocumentReference;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;

import com.regnosys.rosetta.symbols.StaleSymbolIdException;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Function declaration node, corresponding to the {@code function} grammar rule.
 *
 * <p>Represents a {@code func Foo:} declaration with optional dispatch, extension,
 * inputs, output, shortcuts, conditions, operations, and post-conditions.
 *
 * <p>Grammar:
 * <pre>
 * function:
 *     runeAnnotations?                          // P1.4.2 H2 — hoisted from RRootElement, see U008
 *     FUNC qualifiedName (LPAREN validID typeCall ARROW validID RPAREN)?
 *     (EXTENDS qualifiedName)?
 *     definable? COLON
 *     docReference*
 *     annotationRef*
 *     (INPUTS COLON attribute+)?
 *     (OUTPUT COLON attribute)?
 *     shortcutDeclaration*
 *     condition*
 *     operation*
 *     postCondition*
 * ;
 * </pre>
 */
public class RFunction extends RRootElement implements RDefinable {

    /**
     * Discriminator for the three RFunction subspecies — mirrors upstream's
     * {@code com.regnosys.rosetta.ast.RFunction.Origin} enum (rune-dsl
     * 9.75.3) and drives {@code JavaTypeTranslator.toFunctionJavaClass(RFunction)}'s
     * dispatch:
     *
     * <ul>
     *   <li>{@link #FUNCTION} — a plain {@code func} declaration. Routes to
     *       {@code <namespace>.functions/<Name>}.</li>
     *   <li>{@link #REPORT} — a synthetic produced by
     *       {@link #fromReport(RReport)}. Routes to
     *       {@code <namespace>.reports/<body+corpus>ReportFunction}.</li>
     *   <li>{@link #RULE} — a synthetic produced by
     *       {@link #fromRule(RRule)}. Routes to
     *       {@code <namespace>.reports/<Name>Rule}.</li>
     * </ul>
     *
     * <p>Phase X T6.0.5 — added to replace the ad-hoc
     * {@code JavaTypeTranslator.toReportFunctionJavaClass(DottedPath, RReport)}
     * routing introduced at T6. The principled dispatch on {@code Origin}
     * mirrors upstream lines 96-131 of {@code JavaTypeTranslator.java}
     * (rune-dsl 9.75.3) verbatim.
     */
    public enum Origin { FUNCTION, REPORT, RULE }

    private String name;
    private RDispatch dispatch;
    private String superFunctionName;
    private String definition;
    private final List<RAttribute> inputs = new ArrayList<>();
    private RAttribute output;
    private final List<RShortcut> shortcuts = new ArrayList<>();
    private final List<RCondition> conditions = new ArrayList<>();
    private final List<ROperation> operations = new ArrayList<>();
    private final List<RPostCondition> postConditions = new ArrayList<>();

    private final List<RDocReference> docReferences = new ArrayList<>();

    private final List<RAnnotationRef> annotationRefs = new ArrayList<>();

    /**
     * Subspecies discriminator. Defaults to {@link Origin#FUNCTION} so
     * existing call paths ({@code AstBuilder} produces plain {@code func}
     * declarations) see no behavioural change. Set to
     * {@link Origin#RULE} / {@link Origin#REPORT} by
     * {@link #fromRule(RRule)} / {@link #fromReport(RReport)} respectively.
     *
     * <p>Phase X T6.0.5.
     */
    private Origin origin = Origin.FUNCTION;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- dispatch -------------------------------------------------------------

    public Optional<RDispatch> dispatch() {
        return Optional.ofNullable(dispatch);
    }

    public void setDispatch(RDispatch dispatch) {
        checkMutable();
        this.dispatch = dispatch;
    }

    /**
     * The BASE declaration this DISPATCH VARIANT inherits its signature from;
     * empty when this function is not a dispatch variant or no base exists in
     * this file. A dispatch variant ({@code func Foo(param Enum ->
     * VALUE):}) declares only the dispatch parameter in its header; its body
     * resolves inputs and output against the base {@code func Foo:} signature.
     *
     * <p>Upstream semantics mirrored exactly
     * ({@code RosettaFunctionExtensions.getMainFunction}, vendored
     * rune-dsl/rune-lang {@code generator/util/RosettaFunctionExtensions.java:59-67}):
     * the FIRST same-FILE sibling function in document order with the same
     * name and an EMPTY operations list (the signature-only declaration).
     * Upstream's filter admits any Function subtype — dispatch variants
     * included — and does NOT look across files (its own {@code TODO Look-up
     * other Rosetta files?} records the limitation); both quirks are
     * preserved. Consumers: {@code LexicalResolutionPass.buildFunctionScope}
     * (the variant body scope — upstream {@code RosettaScopeProvider}'s
     * {@code getSymbolParentScope} builds Function scopes from the base-aware
     * {@code getInputs}/{@code getOutput}) and
     * {@code TypeInferenceEngine}'s function-input walk. Those upstream
     * accessors REPLACE, not merge: whenever a base exists, a variant's own
     * inputs/output declarations (grammar-legal on both sides) are ignored —
     * every consumer here implements the same replace semantics (the Seat-1
     * #444 MF-3 law; the corpus carries zero variant-own declarations).
     */
    public Optional<RFunction> dispatchBase() {
        if (dispatch == null || name == null) return Optional.empty();
        RNode p = parent();
        while (p != null && !(p instanceof com.regnosys.rosetta.ast.model.RModel)) {
            p = p.parent();
        }
        if (!(p instanceof com.regnosys.rosetta.ast.model.RModel model)) return Optional.empty();
        for (com.regnosys.rosetta.ast.RRootElement elem : model.rootElements()) {
            if (elem != this && elem instanceof RFunction sibling
                    && name.equals(sibling.name()) && sibling.operations().isEmpty()) {
                return Optional.of(sibling);
            }
        }
        return Optional.empty();
    }

    // -- superFunctionName ----------------------------------------------------

    public Optional<String> superFunctionName() {
        return Optional.ofNullable(superFunctionName);
    }

    public void setSuperFunctionName(String superFunctionName) {
        checkMutable();
        this.superFunctionName = superFunctionName;
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

    // -- docReferences --------------------------------------------------------

    public List<RDocReference> docReferences() {
        return docReferences;
    }

    // -- annotationRefs -------------------------------------------------------

    public List<RAnnotationRef> annotationRefs() {
        return annotationRefs;
    }

    // -- origin (Phase X T6.0.5) ----------------------------------------------

    /**
     * Subspecies discriminator. Mirrors upstream's
     * {@code RFunction.getOrigin()} (rune-dsl 9.75.3) verbatim, modulo the
     * bean-less naming convention used by every other accessor on this class
     * (e.g. {@link #name()}, {@link #dispatch()}). Drives
     * {@code JavaTypeTranslator.toFunctionJavaClass(RFunction)}'s 3-way
     * routing per {@link Origin}.
     *
     * <p>Never {@code null} — default value is {@link Origin#FUNCTION}, set
     * by the factories {@link #fromRule(RRule)} (→ {@link Origin#RULE}) and
     * {@link #fromReport(RReport)} (→ {@link Origin#REPORT}).
     *
     * <p>Phase X T6.0.5 — see class javadoc on {@link Origin} for the design
     * rationale that replaces the ad-hoc
     * {@code toReportFunctionJavaClass(DottedPath, RReport)} routing that
     * landed at T6.
     */
    public Origin origin() {
        return origin;
    }

    /**
     * Sets the {@link Origin} discriminator. Package-private — only the
     * {@link #fromRule(RRule)} / {@link #fromReport(RReport)} factories in
     * this package may invoke it. Follows the {@link #checkMutable()}
     * discipline used by every other RFunction setter so freezing remains
     * the single point of invariant enforcement.
     *
     * <p>Phase X T6.0.5.
     */
    void setOrigin(Origin origin) {
        checkMutable();
        this.origin = Objects.requireNonNull(origin, "origin");
    }

    // -- inputs ---------------------------------------------------------------

    public List<RAttribute> inputs() {
        return inputs;
    }

    // -- output ---------------------------------------------------------------

    public Optional<RAttribute> output() {
        return Optional.ofNullable(output);
    }

    public void setOutput(RAttribute output) {
        checkMutable();
        this.output = output;
    }

    // -- shortcuts ------------------------------------------------------------

    public List<RShortcut> shortcuts() {
        return shortcuts;
    }

    // -- conditions -----------------------------------------------------------

    public List<RCondition> conditions() {
        return conditions;
    }

    // -- operations -----------------------------------------------------------

    public List<ROperation> operations() {
        return operations;
    }

    // -- postConditions -------------------------------------------------------

    public List<RPostCondition> postConditions() {
        return postConditions;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        if (dispatch != null) {
            result.add(dispatch);
        }
        result.addAll(docReferences);
        result.addAll(annotationRefs);
        result.addAll(inputs);
        if (output != null) {
            result.add(output);
        }
        result.addAll(shortcuts);
        result.addAll(conditions);
        result.addAll(operations);
        result.addAll(postConditions);
        return List.copyOf(result);
    }

    // === M3 resolved fields (D2) — migrated to SymbolId in P1.4.1b (H7 / U005)

    // Cross-reference key: rune-dsl super-function qualified name resolved to
    // (namespace, localName) at GlobalResolutionPass time. SymbolId carries the
    // workspace generation for staleness detection. See U005.
    @CrossRefField(category = DiagnosticCategory.SUPER_FUNCTION_NOT_FOUND, tokenRangeKey = "superFunction")
    private SymbolId superFunctionId;

    /**
     * Returns the resolved super-function, looked up lazily through the
     * attached workspace. Returns {@link Optional#empty()} if not declared or not
     * resolved.
     *
     * @throws IllegalStateException if this node has no attached workspace
     *     (e.g., constructed manually outside {@code RWorkspace.build(List)})
     * @throws StaleSymbolIdException if the stored SymbolId was issued by
     *     a different workspace generation
     */
    public Optional<RFunction> superFunction() {
        if (superFunctionId == null) return Optional.empty();
        // PR #445: this node is the requester — duplicate FQNs resolve
        // same-file/same-cell first (CellPreference).
        return Optional.ofNullable(workspace().resolve(superFunctionId, RFunction.class, this));
    }

    /**
     * Returns the {@link SymbolId} of the resolved super-function. Use
     * {@link #superFunction()} for the resolved {@link RFunction} node.
     * New in P1.4.1b.
     */
    public Optional<SymbolId> superFunctionId() {
        return Optional.ofNullable(superFunctionId);
    }

    /**
     * Sets the super-function SymbolId. Called by
     * {@code GlobalResolutionPass.resolveSuperFunction}
     * (replaces the removed {@code setResolvedSuperFunction(RFunction)} setter).
     */
    public void setSuperFunctionId(SymbolId id) {
        checkMutable();
        this.superFunctionId = id;
    }

    // === Phase X T2 — static factories (fork analogue to upstream RObjectFactory.buildRFunction)

    /**
     * Phase X T2 — instance field capturing the source {@link RReport} when
     * this RFunction was built via {@link #fromReport(RReport)}. Null
     * otherwise. Accessed by {@code ReportGenerator} (T6) to read the
     * report's regulatory document reference + {@code with type} target.
     *
     * <p>Distinguished from {@code superFunctionId}'s SymbolId-based resolution
     * because the relationship is a 1:1 fork-time bridge (one RFunction
     * directly synthesised from one RReport) rather than a cross-reference
     * resolution.
     */
    private RReport originReport;

    /**
     * The source {@link RRule} when this RFunction was built via
     * {@link #fromRule(RRule)}. {@code null} otherwise. Accessed by
     * {@code RuleGenerator} (T5) to derive the symbol identity (namespace +
     * name) of the synthetic {@link RFunction}, since the synthetic carries
     * no {@link com.regnosys.rosetta.ast.RNode#parent() parent} attachment
     * to an {@link com.regnosys.rosetta.ast.model.RModel RModel} (the
     * source {@link RRule} IS RModel-attached, so we read namespace through
     * it).
     *
     * <p>Mirrors {@link #originReport} verbatim — same 1:1 fork-time bridge
     * pattern. The {@link #fromRule(RRule)} factory sets it; {@code
     * #fromReport(RReport)} does NOT.
     */
    private RRule originRule;

    /**
     * Returns the source {@link RReport} when this {@code RFunction} was
     * built via {@link #fromReport(RReport)}; {@link Optional#empty()}
     * otherwise (including for instances built via {@link #fromRule(RRule)}
     * or constructed directly by {@code AstBuilder}).
     *
     * <p>Phase X T2 — added per plan
     * the development plan "2026-05-19-phase-x-port-3-generators" T2 step 7;
     * used by {@code ReportGenerator} (T6) for emission of the report
     * regulatory metadata. Mirrors the existing {@link #dispatch()} Optional
     * accessor pattern at this class (bean-less property style).
     */
    public Optional<RReport> originReport() {
        return Optional.ofNullable(originReport);
    }

    /**
     * Returns the source {@link RRule} when this {@code RFunction} was built
     * via {@link #fromRule(RRule)}; {@link Optional#empty()} otherwise.
     *
     * <p>Phase X T5 — needed by {@code RuleGenerator.createTypeRepresentation}
     * to recover the source {@link RRule}'s parent {@link
     * com.regnosys.rosetta.ast.model.RModel RModel} for namespace resolution.
     * The synthetic {@link RFunction} produced by {@link #fromRule(RRule)}
     * has no parent attachment (it lives in-memory only, post-link), so
     * {@code generatorModel.symbolId(syntheticFunction)} would throw; the
     * back-pointer here lets {@code RuleGenerator} bypass that by reading
     * namespace off the source {@link RRule} (which IS RModel-attached).
     *
     * <p>Mirrors {@link #originReport()} verbatim — see that accessor for
     * the precedent.
     */
    public Optional<RRule> originRule() {
        return Optional.ofNullable(originRule);
    }

    /**
     * Sets the source {@link RReport}. Package-private — only the
     * {@link #fromReport(RReport)} factory in this package may invoke it.
     * Follows the {@link #checkMutable()} discipline used by every other
     * RFunction setter so freezing remains the single point of invariant
     * enforcement.
     */
    void setOriginReport(RReport originReport) {
        checkMutable();
        this.originReport = originReport;
    }

    /**
     * Sets the source {@link RRule}. Package-private — only the
     * {@link #fromRule(RRule)} factory in this package may invoke it. See
     * {@link #setOriginReport(RReport)} for the mirrored precedent +
     * {@link #checkMutable()} discipline rationale.
     */
    void setOriginRule(RRule originRule) {
        checkMutable();
        this.originRule = originRule;
    }

    /**
     * Build an {@link RFunction} representation from an {@link RRule} AST
     * node. Fork analogue to upstream
     * {@code RObjectFactory.buildRFunction(RosettaRule)}
     * (rune-dsl 9.75.3 lines 103-124).
     *
     * <p>The returned function carries:
     * <ul>
     *   <li>name: {@code rule.name()}</li>
     *   <li>definition: {@code rule.definition()} if present</li>
     *   <li>one synthetic input attribute named {@code "input"} whose
     *       typeCall mirrors {@code rule.fromType()} (the rule's source
     *       data type)</li>
     *   <li>one synthetic output attribute named {@code "output"} (typeCall
     *       null at build-time; resolved at use-site via the rule's expression
     *       type)</li>
     *   <li>originReport: {@code null} (this factory is for rules, not reports)</li>
     *   <li>originRule: the source {@link RRule} (accessible via
     *       {@link #originRule()} — needed by {@code RuleGenerator} (T5)
     *       to recover the RModel-attached parent for namespace resolution
     *       since the synthetic {@link RFunction} has no parent attachment
     *       itself).</li>
     * </ul>
     *
     * <p>Inputs/output cardinality defaults to {@code (0..1)} mirroring
     * upstream's {@code createArtificialAttribute} pattern (lines 92-102).
     */
    public static RFunction fromRule(RRule rule) {
        Objects.requireNonNull(rule, "rule must not be null");
        RFunction func = new RFunction();
        func.setName(rule.name());
        rule.definition().ifPresent(func::setDefinition);

        // Synthetic input attribute mirroring upstream's createArtificialAttribute("input", ...).
        RAttribute input = newSyntheticAttribute("input",
                rule.fromType().map(RFunction::copyTypeCall).orElse(null),
                0, 1);
        func.inputs().add(input);

        // Synthetic output attribute. typeCall left null at construction time —
        // RuleGenerator.generate back-fills it from the expression's inferred
        // type at codegen time (Phase X1).
        RAttribute output = newSyntheticAttribute("output", null, 0, 1);
        func.setOutput(output);

        // Phase X1 — synthesise ROperation(SET, "output", expression) when the
        // rule carries an expression. Mirrors upstream
        // RObjectFactory.buildRFunction(RosettaRule) line 121.
        if (rule.expression().isPresent()) {
            ROperation op = new ROperation();
            op.setOperator(OperationOp.SET);
            op.setTargetName("output");
            op.setExpression(rule.expression().get());
            func.operations().add(op);
        }

        func.setOriginRule(rule);
        func.setOrigin(Origin.RULE);
        return func;
    }

    /**
     * Build an {@link RFunction} representation from an {@link RReport} AST
     * node. Fork analogue to upstream
     * {@code RObjectFactory.buildRFunction(RosettaReport)}
     * (rune-dsl 9.75.3 lines 126-160).
     *
     * <p>The returned function carries:
     * <ul>
     *   <li>name: synthetic name derived from the report's regulatory body
     *       + corpus list (matches upstream's
     *       {@code report.getRegulatoryBody().getBody().getName() + " " + corpusJoin}
     *       pattern at line 127-131)</li>
     *   <li>one synthetic input attribute named {@code "input"} whose
     *       typeCall is a deep-copy of {@code report.fromType()}</li>
     *   <li>one synthetic output attribute named {@code "output"} whose
     *       typeCall is a fresh {@link RTypeCall} with typeName
     *       {@code report.withType()}</li>
     *   <li>originReport: the source {@link RReport} (accessible via
     *       {@link #originReport()} — needed by ReportGenerator at T6
     *       step 3 to surface the regulatory metadata on emission).</li>
     *   <li>definition: not set on the synthetic function. The report's
     *       definition (when present) lives on the source {@link RReport};
     *       callers needing it read it through {@link #originReport()}.</li>
     * </ul>
     *
     * <p>Fails fast with {@link IllegalArgumentException} when
     * {@code report.fromType()} is null OR {@code report.withType()} is
     * null/empty. Both fields are <b>required</b> by the Rosetta grammar
     * (RosettaReport production: {@code 'from' inputType=TypeCall} and
     * {@code 'with' 'type' reportType=[Data|QualifiedName]} — neither
     * carries the {@code ?} optionality marker). A null on either field
     * means the input AST is grammar-violating; allowing it through would
     * silently produce a synthetic output attribute with no typeCall, which
     * the codegen pipeline resolves to {@code RMissingType} and emits as
     * {@code ReportFunction<Object, FieldWithMeta<Object>>} — a wrong-typed
     * generic signature that fails byte-parity against the legacy plugin
     * output. Failing fast at the factory boundary turns a silent codegen
     * regression into a loud test failure with a meaningful message. This
     * mirrors the existing fail-fast at {@link #deriveReportName(RReport)}
     * for the {@code bodyRef} field.
     */
    public static RFunction fromReport(RReport report) {
        Objects.requireNonNull(report, "report must not be null");
        if (report.fromType() == null) {
            throw new IllegalArgumentException(
                    "report must have fromType set (grammar mandates "
                    + "'from' inputType=TypeCall); got: " + report);
        }
        if (report.withType() == null || report.withType().isEmpty()) {
            throw new IllegalArgumentException(
                    "report must have withType set (grammar mandates "
                    + "'with' 'type' reportType=[Data|QualifiedName]); got: "
                    + report);
        }
        RFunction func = new RFunction();
        func.setName(deriveReportName(report));
        // No analogue to upstream's reportDefinition string assembly is
        // wired through to the RFunction.definition() field — the fork's
        // report definition lives on RReport itself; ReportGenerator can
        // pull it directly via originReport() when needed.

        // Synthetic input attribute mirroring upstream's
        // EcoreUtil2.copy(report.getInputType()) pattern (line 138).
        // fromType() guaranteed non-null by the fail-fast above.
        RAttribute input = newSyntheticAttribute("input",
                copyTypeCall(report.fromType()),
                0, 1);
        func.inputs().add(input);

        // Synthetic output attribute named "output" whose typeCall references
        // the report's `with type` target by name. withType() guaranteed
        // non-null/non-empty by the fail-fast above.
        RTypeCall outTc = new RTypeCall();
        outTc.setTypeName(report.withType());
        // v3.2 seat 4 (PR #625, F7): the output is typed BY the linker's id, exactly as the input is
        // back-filled from the report's fromType at codegen — never by the name alone, which the
        // generator would resolve by a workspace-wide first match (the chaos s07 namespace leak)
        // round 1 (the spec review's SF-3, fail-CLOSED): the id is set ONLY when the report's resolver could
        // be shared (RNode.shareResolverWith - the ONE declaration every synthetic builder consults); a
        // detached report (never attached, or its workspace collected) yields an id-LESS output, which both
        // report seats REFUSE loudly at REPORT_REFERENCE_UNRESOLVED - never an id on a detached node, whose
        // first referencedType() read threw "never attached" and was rendered as a TODO comment
        if (report.shareResolverWith(outTc)) {
            report.withTypeId().ifPresent(outTc::setReferencedTypeId);
        }
        RAttribute output = newSyntheticAttribute("output", outTc, 0, 1);
        func.setOutput(output);

        func.setOriginReport(report);
        func.setOrigin(Origin.REPORT);
        return func;
    }

    // -- Private helpers for the static factories -----------------------------

    private static RAttribute newSyntheticAttribute(String name, RTypeCall typeCall,
                                                    int inf, int sup) {
        RAttribute attr = new RAttribute();
        attr.setName(name);
        if (typeCall != null) {
            attr.setTypeCall(typeCall);
        }
        RCardinality card = new RCardinality();
        card.setInf(inf);
        card.setSup(sup);
        attr.setCardinality(card);
        return attr;
    }

    /**
     * Deep-copy of an {@link RTypeCall} — mirrors upstream's
     * {@code EcoreUtil2.copy(report.getInputType())}. Delegates to
     * {@link RTypeCall#deepCopy} which produces fresh argument +
     * argument-expression nodes so the synthetic copy doesn't alias the
     * source's AST subtree (Copilot PR #72 R4 F10 — sharing
     * {@link RTypeCallArgument} nodes between two type calls would conflict
     * with the {@link com.regnosys.rosetta.ast.RNode} single-parent contract).
     * Resolution state ({@code referencedTypeId}) is NOT copied — the synthetic
     * function's input attribute starts unresolved so the linker handles it
     * independently.
     */
    private static RTypeCall copyTypeCall(RTypeCall source) {
        return RTypeCall.deepCopy(source);
    }

    /**
     * Derive a synthetic name for a report-backed RFunction. Mirrors
     * upstream's report-id alphanumeric derivation at
     * {@code ModelReportId.joinRegulatoryReference()} — concatenates the
     * regulatory body reference with every corpus reference (no separator),
     * e.g. {@code ASIC} + {@code Margin} → {@code ASICMargin}.
     *
     * <p><b>Phase X T6 — byte-parity requirement.</b> This name flows into
     * two downstream class-naming computations that BOTH must match upstream's
     * legacy plugin output for D11 byte-parity:
     * <ul>
     *   <li>{@code JavaTypeTranslator.toFunctionJavaClass(RFunction, ModelSymbolId)}
     *       — for {@code RFunction.origin() == REPORT} (T6.0.5) — adds a
     *       {@code ReportFunction} suffix, producing e.g.
     *       {@code ASICMarginReportFunction} under the {@code <ns>.reports}
     *       package (see {@code drr/regulation/asic/rewrite/margin/reports/
     *       ASICMarginReportFunction.java} for a worked golden).</li>
     *   <li>{@code JavaTypeTranslator.toLabelProviderJavaClass(ModelSymbolId)}
     *       adds a {@code LabelProvider} suffix, producing e.g.
     *       {@code ASICMarginLabelProvider} under the {@code <ns>.labels}
     *       package. The same golden file imports
     *       {@code ASICMarginLabelProvider} on line 9 — confirming the
     *       alphanumeric (body+corpus) derivation is the right input to
     *       {@code toLabelProviderJavaClass} for report-origin functions.</li>
     * </ul>
     * The pre-T6 implementation returned {@code withType} (e.g.
     * {@code ASICMarginReport}) which produced wrongly-suffixed
     * {@code ASICMarginReportReportFunction} +
     * {@code ASICMarginReportLabelProvider} class names — corrected here as
     * part of T6 to keep T7/T8 byte-parity-ready.
     *
     * <p>Fails fast with {@link IllegalArgumentException} when
     * {@code report.regulatoryDocRef()} or its {@code bodyRef} is absent:
     * returning a literal fallback would collide across reports and produce
     * non-unique RFunction names that break downstream emission.
     */
    private static String deriveReportName(RReport report) {
        RRegulatoryDocumentReference ref = report.regulatoryDocRef();
        if (ref == null || ref.bodyRef() == null || ref.bodyRef().isEmpty()) {
            throw new IllegalArgumentException(
                    "report must have regulatoryDocRef.bodyRef set; got: " + report);
        }
        StringBuilder name = new StringBuilder(ref.bodyRef());
        for (String corpus : ref.corpusRefs()) {
            if (corpus != null) {
                name.append(corpus);
            }
        }
        return name.toString();
    }
}
