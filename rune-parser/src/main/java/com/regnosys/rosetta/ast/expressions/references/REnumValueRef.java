package com.regnosys.rosetta.ast.expressions.references;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

/**
 * Enum value reference expression node.
 *
 * <p>Represents a reference to a specific enum value, e.g.,
 * {@code TradeTypeEnum -> NewTrade}. Stores both the enum name
 * and the value name as strings.
 *
 * <p>EMF equivalent: {@code RosettaSymbolReference} targeting an enum value.
 */
public class REnumValueRef extends RExpression {

    private String enumName;
    private String valueName;

    // -- enumName -------------------------------------------------------------

    public String enumName() {
        return enumName;
    }

    public void setEnumName(String enumName) {
        checkMutable();
        this.enumName = enumName;
    }

    // -- valueName ------------------------------------------------------------

    public String valueName() {
        return valueName;
    }

    public void setValueName(String valueName) {
        checkMutable();
        this.valueName = valueName;
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.ENUM_NOT_FOUND)
    private com.regnosys.rosetta.ast.types.REnumeration resolvedEnum;

    @CrossRefField(category = DiagnosticCategory.ENUM_VALUE_NOT_FOUND)
    private com.regnosys.rosetta.ast.supporting.REnumValue resolvedValue;

    public java.util.Optional<com.regnosys.rosetta.ast.types.REnumeration> enumeration() { return java.util.Optional.ofNullable(resolvedEnum); }
    public void setResolvedEnum(com.regnosys.rosetta.ast.types.REnumeration resolved) { checkMutable(); this.resolvedEnum = resolved; }

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.REnumValue> enumValue() { return java.util.Optional.ofNullable(resolvedValue); }
    public void setResolvedValue(com.regnosys.rosetta.ast.supporting.REnumValue resolved) { checkMutable(); this.resolvedValue = resolved; }

    // === Phase X1 closure T0h (Gap A) — LHS resolves to a callable symbol =====
    //
    // The grammar parses `<func-name> -> <feature>` as REnumValueRef
    // (enumName="<func-name>", valueName="<feature>") per the
    // {EnumName -> ValueName} production precedence. When the LHS is a
    // workspace func/rule (not an enum), GlobalResolutionPass binds the
    // resolved symbol here instead of emitting ENUM_NOT_FOUND, and Cat 11
    // in TypeInferenceEngine reads it to compute the feature lookup against
    // the symbol's output type.
    //
    // <p>Spec: T0h evidence at the development audit "phase-x1-T0g-rextract-rthen-deepest-missing-2026-05-23" § 3 Gap A.
    private com.regnosys.rosetta.ast.RRootElement resolvedSymbol;

    public java.util.Optional<com.regnosys.rosetta.ast.RRootElement> resolvedSymbol() {
        return java.util.Optional.ofNullable(resolvedSymbol);
    }

    public void setResolvedSymbol(com.regnosys.rosetta.ast.RRootElement symbol) {
        checkMutable();
        this.resolvedSymbol = symbol;
    }

    // === v3.1 C1 — THE AUTHORITATIVE FEATURE BINDING (spec R9) ==============
    //
    // What the RIGHT-hand name of `a -> b` actually names, from the one feature
    // lookup: an RAttribute, an RChoiceOption, a meta type or a record feature.
    // The older slots hold narrower views of the same answer — resolvedChoiceOption
    // holds the option's TYPE rather than the option, which loses which choice it
    // came through — and are kept because 409 call sites read them. Retiring them
    // in favour of this one is a recorded follow-on.
    private com.regnosys.rosetta.ast.RNode resolvedFeatureNode;

    public java.util.Optional<com.regnosys.rosetta.ast.RNode> resolvedFeatureNode() {
        return java.util.Optional.ofNullable(resolvedFeatureNode);
    }

    public void setResolvedFeatureNode(com.regnosys.rosetta.ast.RNode resolved) {
        checkMutable();
        this.resolvedFeatureNode = resolved;
    }

    // === v3.1 C1 — THE HEAD BINDING (the slot upstream has and this node lacked)

    // Upstream parses `a -> b` in an expression as a RosettaFeatureCall whose
    // RECEIVER is an ordinary RosettaSymbolReference, so the left name gets a
    // real binding like any other name. The fork's grammar makes `a -> b` an
    // REnumValueRef instead, and until now the left name got NO binding at all
    // in the common case: for `<input> -> <feature>` the engine resolved the
    // FEATURE through {@link #resolvedInputFeature} (documented typing-only)
    // and recorded nothing for the head. Measured by
    // ResolutionConformanceTest, that was 23 of 51 differences from upstream —
    // the single largest class.
    //
    // <p>This field is that missing binding, resolved by the ONE symbol rule of
    // {@code docs/specs/2026-08-15-upstream-resolution-spec.md} § 3 (R3-R7):
    // the lexical parent chain innermost-first, then the implicit item's
    // features, then the expected type's enum values. It holds whatever the
    // name legitimately names — an RAttribute (function input, item feature),
    // an RShortcut (alias), an RClosureParameter (a declared lambda parameter —
    // upstream's ClosureParameter node, since v3.2 seat 8; a name-only parameter
    // still binds its declaring RInlineFunction), an REnumeration, an RFunction
    // or an RRule.
    //
    // <p>ADDITIVE by design. The eight resolution slots around it are the rungs
    // of the ladder this replaces, and they have 409 call sites across 99 files;
    // collapsing them is a recorded follow-on, not C1. Until then this is the
    // AUTHORITY for what the left name means, and the rungs are filled FROM it.
    private com.regnosys.rosetta.ast.RNode resolvedHead;

    public java.util.Optional<com.regnosys.rosetta.ast.RNode> resolvedHead() {
        return java.util.Optional.ofNullable(resolvedHead);
    }

    public void setResolvedHead(com.regnosys.rosetta.ast.RNode head) {
        checkMutable();
        this.resolvedHead = head;
    }

    // === Phase X1 closure T0i (Gap B) — attribute → type-restriction =========
    //
    // The grammar parses `<attribute-name> -> <SubtypeName>` as REnumValueRef
    // (enumName=<attribute-name>, valueName=<SubtypeName>) per the
    // {EnumName -> ValueName} production precedence. In Rosetta semantics this
    // is the *type-restriction (downcast)* operator — narrow the polymorphic
    // attribute to its declared subtype (e.g. `payout -> OptionPayout` on a
    // `payout : Payout` attribute restricts to the `OptionPayout` subtype).
    //
    // Resolution splits across phases:
    //   Phase A (GlobalResolutionPass.resolveEnumValueRef) speculatively binds
    //     {@code resolvedRestrictionType} when {@code valueName} resolves to a
    //     workspace RDataType, but does NOT skip ENUM_NOT_FOUND and does NOT
    //     register a cross-ref — both are deferred to Phase B on a verified bind.
    //     (Differs from the T0h Gap A pattern, whose LHS-callable bind is
    //     conclusive and so skips ENUM_NOT_FOUND already at Phase A.)
    //   Phase B (TypeInferenceEngine Cat 12) verifies the LHS attribute lookup
    //     on item-type + subtype check via SubtypeRelation; on success it binds
    //     the full {@link TypeRestriction} record below, registers the cross-ref,
    //     and clears ENUM_NOT_FOUND. On failure the ENUM_NOT_FOUND diagnostic
    //     stands (no silent failure for a genuine typo like `foo -> Trade`).
    //
    // {@link ExpressionTypeComputer#computeEnumValueRef} reads ONLY the
    // verified {@code resolvedTypeRestriction} record; the speculative
    // {@code resolvedRestrictionType} is the Phase-A→Phase-B handoff field.
    //
    // <p>Spec: T0i evidence at the development audit "phase-x1-T0g-rextract-rthen-deepest-missing-2026-05-23" § 3 Gap B.
    private com.regnosys.rosetta.ast.types.RDataType resolvedRestrictionType;

    public java.util.Optional<com.regnosys.rosetta.ast.types.RDataType> resolvedRestrictionType() {
        return java.util.Optional.ofNullable(resolvedRestrictionType);
    }

    public void setResolvedRestrictionType(com.regnosys.rosetta.ast.types.RDataType type) {
        checkMutable();
        this.resolvedRestrictionType = type;
    }

    private TypeRestriction resolvedTypeRestriction;

    public java.util.Optional<TypeRestriction> resolvedTypeRestriction() {
        return java.util.Optional.ofNullable(resolvedTypeRestriction);
    }

    public void setResolvedTypeRestriction(TypeRestriction restriction) {
        checkMutable();
        this.resolvedTypeRestriction = restriction;
    }

    // === Phase X1 closure T10 — choice-option navigation =====================
    //
    // `<choiceAttr> -> <OptionName>` narrows a choice-typed attribute to one of
    // the choice's declared options (e.g. `payout -> CommodityPayout` where
    // `payout : Payout`, `Payout` is a choice, and `CommodityPayout` is one of
    // its options). The grammar parses this as REnumValueRef.
    //
    // <p>Distinct from Gap B ({@link #resolvedRestrictionType}): Gap B relies on
    // GlobalResolutionPass resolving <OptionName> as a workspace GLOBAL symbol in
    // the rule file's scope, which fails when the option type is not imported
    // there ({@code restr=false} for the bulk of the DRR `payout -> CommodityPayout`
    // residual). The choice's option list is always in scope (it IS part of the
    // choice type), so TypeInferenceEngine resolves the narrowing locally and
    // binds the matched option node here. The option may itself be a data type
    // OR a nested choice, so the resolved node is the common {@link
    // com.regnosys.rosetta.ast.RRootElement} supertype ({@code RDataType} |
    // {@code RChoice}); {@link com.regnosys.rosetta.types.inference.ExpressionTypeComputer#computeEnumValueRef}
    // converts it back to the corresponding {@code RType} via {@code astNodeToRType}.
    private com.regnosys.rosetta.ast.RRootElement resolvedChoiceOption;

    public java.util.Optional<com.regnosys.rosetta.ast.RRootElement> resolvedChoiceOption() {
        return java.util.Optional.ofNullable(resolvedChoiceOption);
    }

    public void setResolvedChoiceOption(com.regnosys.rosetta.ast.RRootElement option) {
        checkMutable();
        this.resolvedChoiceOption = option;
    }

    // PR #455 (facet warningFamilyWaves): bind-time record of whether the
    // option nav's HEAD (the receiver upstream's parse gives the enclosing
    // RosettaFeatureCall) typed as a DIRECT choice (RChoiceTypeRef) rather
    // than a `type X extends <choice>` data type admitted through the #451
    // choiceViewOfType walk. Upstream's checkPathOperatorOnChoice fires only
    // on choice-TYPED receivers, so the path-operator deprecation arm
    // consults this bit — a view-admitted data-type head stays silent
    // exactly as upstream's does.
    private boolean choiceOptionHeadIsDirectChoice;

    public boolean choiceOptionHeadIsDirectChoice() {
        return choiceOptionHeadIsDirectChoice;
    }

    public void setChoiceOptionHeadIsDirectChoice(boolean directChoice) {
        checkMutable();
        this.choiceOptionHeadIsDirectChoice = directChoice;
    }

    /**
     * Verified type-restriction (downcast) bind for the {@code attribute -> Subtype}
     * shape. Holds the LHS attribute on item-type ({@code lhsAttribute}) and the
     * RHS restriction target ({@code restrictionType}). Both are mandatory in a
     * Phase-B-verified bind; the Phase-A speculative bind uses
     * {@link REnumValueRef#resolvedRestrictionType()} (a separate field) instead.
     */
    public static final class TypeRestriction {
        private final com.regnosys.rosetta.ast.supporting.RAttribute lhsAttribute;
        private final com.regnosys.rosetta.ast.types.RDataType restrictionType;

        public TypeRestriction(
                com.regnosys.rosetta.ast.supporting.RAttribute lhsAttribute,
                com.regnosys.rosetta.ast.types.RDataType restrictionType) {
            this.lhsAttribute = java.util.Objects.requireNonNull(lhsAttribute, "lhsAttribute");
            this.restrictionType = java.util.Objects.requireNonNull(restrictionType, "restrictionType");
        }

        /** Always non-null in a Phase-B-verified bind (the ctor requires it). */
        public com.regnosys.rosetta.ast.supporting.RAttribute lhsAttribute() {
            return lhsAttribute;
        }

        public com.regnosys.rosetta.ast.types.RDataType restrictionType() {
            return restrictionType;
        }
    }

    // === Phase X1 Category 10 — attribute-feature fallback ====================
    //
    // The grammar parses `a -> b` as REnumValueRef (enumName="a", valueName="b")
    // because the {EnumName -> ValueName} production has precedence. When the
    // enum resolution fails AND the node sits inside an implicit inline body,
    // the M3 fixed-point inference (Category 10) reinterprets it as an
    // attribute-feature chain on the implicit-item type: enumName becomes an
    // attribute lookup on the implicit-item, and valueName becomes a feature
    // lookup on that attribute's type. The resolved pair lives here.
    //
    // <p>Spec: docs/superpowers/specs/2026-05-21-phase-x1-m3-implicit-input-extension.md (local).

    private AttributeChain resolvedAttributeChain;

    public java.util.Optional<AttributeChain> resolvedAttributeChain() {
        return java.util.Optional.ofNullable(resolvedAttributeChain);
    }

    public void setResolvedAttributeChain(AttributeChain chain) {
        checkMutable();
        this.resolvedAttributeChain = chain;
    }

    // === IR-Lab cascade fix (2026-06-27) — lexical-head nav ===================
    //
    // The grammar parses `<name> -> <feature>` as REnumValueRef(enumName=name,
    // valueName=feature). When the leading name resolves in the LEXICAL context —
    // the enclosing FUNCTION's input (by param name OR the implicit
    // lowercase-input-type-name convention; the original 2026-06-27 shape), one of
    // the enclosing FUNCTION's shortcuts (the alias head, PR #443), or an
    // attribute of the data type DECLARING the enclosing condition (the
    // condition-context head, PR #443) — the nav reads <feature> on the head's
    // type. The function-input form is the function analogue of the rule
    // from-type implicit scope (which {@code getEnclosingItemType} already handles)
    // and of the closure-param one-segment {@link AttributeChain}. The engine had
    // no lexical-head fallback, so these navs typed MISSING (the dominant IR-Lab
    // head->feature cascade; the #443 census's ENUM_NOT_FOUND mass).
    //
    // <p>This is a TYPING-ONLY field, deliberately SEPARATE from
    // {@link #resolvedAttributeChain} (which the Path-1 generator reads to RENDER
    // a nav): {@link com.regnosys.rosetta.types.inference.ExpressionTypeComputer}
    // reads it to expose the resolved type at {@code getInferredType} (the path the
    // IR adapter calls), while the generator does NOT bind it as a nav. This is a
    // separation-of-concerns boundary, NOT a byte-neutrality claim — the generator
    // still consults {@code getInferredType}, so the better type IS byte-positive
    // (the PR #279 +6 FUNCTION flips). NOT a
    // {@link com.regnosys.rosetta.symbols.linker.CrossRefField} (it is an
    // inference-stage resolution, not a linker cross-ref).
    private com.regnosys.rosetta.ast.supporting.RAttribute resolvedInputFeature;

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.RAttribute> resolvedInputFeature() {
        return java.util.Optional.ofNullable(resolvedInputFeature);
    }

    public void setResolvedInputFeature(com.regnosys.rosetta.ast.supporting.RAttribute feature) {
        checkMutable();
        this.resolvedInputFeature = feature;
    }

    /**
     * Chain bound by Category 10 fallback resolution. Two shapes:
     * <ul>
     *   <li><b>Two-segment</b> (implicit-item case): {@code attribute} is the
     *       first segment (looked up on the implicit-item type); {@code feature}
     *       is the second segment (looked up on the first segment's element
     *       type). Use the two-arg constructor.</li>
     *   <li><b>One-segment closure-param</b> (D42 LOCK 2026-05-23): the leading
     *       source name matched an explicit closure parameter (no real
     *       {@link com.regnosys.rosetta.ast.supporting.RAttribute} backs it),
     *       so {@code attribute} is {@code null} and {@code feature} is the
     *       trailing name resolved against the closure param's type (== the
     *       chain op's argument element type). Use the one-arg constructor.</li>
     * </ul>
     * {@code feature} is always non-null; consumers may read {@code attribute}
     * via {@link #attributeOpt()} to disambiguate the two shapes.
     */
    public static final class AttributeChain {
        private final com.regnosys.rosetta.ast.supporting.RAttribute attribute;
        private final com.regnosys.rosetta.ast.supporting.RAttribute feature;

        public AttributeChain(
                com.regnosys.rosetta.ast.supporting.RAttribute attribute,
                com.regnosys.rosetta.ast.supporting.RAttribute feature) {
            this.attribute = java.util.Objects.requireNonNull(attribute, "attribute");
            this.feature = java.util.Objects.requireNonNull(feature, "feature");
        }

        /**
         * One-segment closure-param construction: no first-segment attribute
         * (the leading source name matched an explicit closure parameter, which
         * is a local variable not an attribute). {@code feature} resolves on
         * the closure param's type.
         */
        public AttributeChain(com.regnosys.rosetta.ast.supporting.RAttribute feature) {
            this.attribute = null;
            this.feature = java.util.Objects.requireNonNull(feature, "feature");
        }

        /**
         * Returns the first-segment attribute, or {@code null} for the
         * one-segment closure-param shape. Prefer {@link #attributeOpt()}.
         */
        public com.regnosys.rosetta.ast.supporting.RAttribute attribute() { return attribute; }

        /** Returns the first-segment attribute as an {@link java.util.Optional}. */
        public java.util.Optional<com.regnosys.rosetta.ast.supporting.RAttribute> attributeOpt() {
            return java.util.Optional.ofNullable(attribute);
        }

        public com.regnosys.rosetta.ast.supporting.RAttribute feature()   { return feature; }
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitEnumValueRef(this, context);
    }
}
