package com.regnosys.rosetta.ast.supporting;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Type reference node, corresponding to the {@code typeCall} grammar rule.
 *
 * <p>A type call references a named type with optional type arguments.
 *
 * <p>Grammar:
 * <pre>
 * typeCall:
 *     qualifiedName (LPAREN arguments+=typeCallArgument (COMMA arguments+=typeCallArgument)* RPAREN)?
 * ;
 * </pre>
 */
public class RTypeCall extends RNode {

    private String typeName;
    private final List<RTypeCallArgument> arguments = new ArrayList<>();

    // -- typeName -------------------------------------------------------------

    public String typeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        checkMutable();
        this.typeName = typeName;
    }

    // -- arguments ------------------------------------------------------------

    /**
     * Returns the type call's arguments.
     *
     * <p><strong>Mutable backing list — by design.</strong> Per the
     * {@link RNode} mutation contract, AST list fields are exposed directly
     * (not defensively copied) so the {@code AstBuilder} can populate them
     * during construction. After construction the list is treated as
     * effectively immutable; consumers should not mutate it. Use
     * {@link #children()} (which returns an unmodifiable view) for tree
     * traversal.
     */
    public List<RTypeCallArgument> arguments() {
        return arguments;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        return List.copyOf(arguments);
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.TYPE_NOT_FOUND, tokenRangeKey = "typeName")
    private SymbolId referencedTypeId;

    /**
     * Returns the resolved type, looked up lazily through the attached workspace.
     * Returns Optional.empty() if not declared or not resolved. The returned RNode
     * is the broadest possible type — callers may need instanceof checks for the
     * specific kind (RDataType, REnumeration, RChoice, RTypeAlias, RBasicType,
     * RRecordType).
     *
     * @throws IllegalStateException if this node has no attached workspace
     * @throws com.regnosys.rosetta.symbols.StaleSymbolIdException if the stored SymbolId was issued by a different
     *     workspace generation
     */
    public Optional<RNode> referencedType() {
        if (referencedTypeId == null) return Optional.empty();
        // Use resolveTypeLike (not resolve) so a rule/function/etc. sharing
        // the same localName in the namespace does not shadow the type via
        // the first-wins lookup. Concrete repro: namespace drr.enrichment.common
        // declares both `reporting rule EnrichmentData` and `type EnrichmentData`;
        // the attribute typeCall must resolve to the type even when the rule
        // is registered first.
        // PR #445: this node is the requester — duplicate FQNs resolve
        // same-file/same-cell first (CellPreference).
        return Optional.ofNullable(workspace().resolveTypeLike(referencedTypeId, this));
    }

    /**
     * Returns the {@link SymbolId} of the resolved type. Use {@link #referencedType()}
     * for the resolved RNode. New in P1.4.1b.
     */
    public Optional<SymbolId> referencedTypeId() {
        return Optional.ofNullable(referencedTypeId);
    }

    public void setReferencedTypeId(SymbolId id) {
        checkMutable();
        this.referencedTypeId = id;
    }

    /**
     * Deep-copy factory — returns a fresh {@link RTypeCall} that owns its own
     * {@link RTypeCallArgument} + {@link RTypeCallArgumentExpression} nodes.
     * Does NOT copy resolution state ({@link #referencedTypeId}) — the
     * synthetic copy starts unresolved so the linker handles it independently.
     *
     * <p>Copilot PR #72 R4 F9+F10 — both {@code RFunction.copyTypeCall} and
     * {@code RChoiceTypeRef.asRDataType} were creating shared-subtree AST by
     * reusing the source's argument nodes (or, in the choice-projection
     * case, the source's entire {@link RTypeCall}). The
     * shared-subtree shape conflicts with {@link RNode}'s
     * single-parent contract via {@code setParent}'s {@code checkMutable()}
     * guard. This helper produces an unfrozen subtree that callers can wire
     * into their own parent chain without aliasing the source.
     */
    public static RTypeCall deepCopy(RTypeCall source) {
        if (source == null) return null;
        RTypeCall copy = new RTypeCall();
        copy.setTypeName(source.typeName());
        for (RTypeCallArgument srcArg : source.arguments()) {
            RTypeCallArgument argCopy = new RTypeCallArgument();
            argCopy.setParameterName(srcArg.parameterName());
            RTypeCallArgumentExpression srcValue = srcArg.value();
            if (srcValue != null) {
                RTypeCallArgumentExpression valueCopy = new RTypeCallArgumentExpression();
                valueCopy.setNegated(srcValue.isNegated());
                srcValue.nameValue().ifPresent(valueCopy::setNameValue);
                srcValue.literalValue().ifPresent(valueCopy::setLiteralValue);
                argCopy.setValue(valueCopy);
            }
            copy.arguments().add(argCopy);
        }
        return copy;
    }
}
