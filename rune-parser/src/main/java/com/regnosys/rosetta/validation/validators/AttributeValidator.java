package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.inference.TypeInferenceEngine;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Validates attribute override declarations against upstream
 * {@code AttributeValidator.checkAttributeOverride}'s semantics (vendored
 * rune-dsl {@code validation/AttributeValidator.java:81-131}), messages
 * byte-identical:
 * <ul>
 *   <li>the parent attribute must exist in the supertype chain — otherwise
 *       {@code Attribute <name> does not exist in supertype} (upstream's single
 *       message covers both the no-{@code extends} case and the
 *       name-not-found case: {@code getParentAttribute()} is null in both);</li>
 *   <li>the overridden type must be a SUBTYPE of the parent attribute's type
 *       (restriction is legal — the PR #442 B-1 heal; the pre-#442 fork seat
 *       compared raw type-call name strings for equality, erroring 171 legal
 *       DRR restriction overrides plus every same-type override merely spelled
 *       with a different namespace qualification) — otherwise
 *       {@code The overridden type should be a subtype of the parent type <T>}.</li>
 * </ul>
 *
 * <p><b>The decline gates (the #437 law — never fabricate on what the fork
 * cannot resolve):</b> the type check declines when either attribute type
 * resolves MISSING; the not-found error fires only when the supertype chain is
 * COMPLETE (every {@code extends} link resolved to its root) — a chain broken
 * by an unresolved supertype cannot prove absence. The broken-chain decline is
 * a DELIBERATE recorded divergence: upstream's {@code getSuperType()} returns
 * null on an unresolved proxy, so upstream DOES emit the not-found error on a
 * broken-extends model (alongside the linker's own error) — but the fork's
 * snapshot corpora carry tens of thousands of stale linking failures that are
 * merged-workspace artifacts, not per-cell model defects, and mirroring
 * upstream here would fabricate not-found errors at corpus scale.
 *
 * <p><b>Choice parents (upstream's documented #797 limitation, reproduced for
 * drop-in parity):</b> upstream's seat calls {@code isSubtypeOf} with its
 * default {@code treatChoiceTypesAsDataTypes=true}, which converts a choice to
 * its data view BEFORE any option rule can fire — the same choice admits by
 * identity, while narrowing to an option or a sub-choice REJECTS (upstream's
 * own {@code testCannotOverrideChoiceTypeToOption} witnesses). The fork's
 * shared {@link SubtypeRelation} Rules 7/8 would admit those narrowings, so
 * this seat guards choices locally instead of consulting the shared relation.
 *
 * <p>Upstream's remaining override arms (cardinality-not-broader, inherited
 * rule-reference compatibility, the function-input-override rejection, and the
 * deprecation-propagation warning) are false-NEGATIVE gaps with zero corpus
 * carriers — recorded for the Annex-A incremental-adopt program, not this
 * seat's scope.
 */
public final class AttributeValidator implements Validator {

    private final TypeInferenceEngine typeEngine;
    private final SubtypeRelation subtypeRelation;

    public AttributeValidator(TypeInferenceEngine typeEngine, SubtypeRelation subtypeRelation) {
        this.typeEngine = typeEngine;
        this.subtypeRelation = subtypeRelation;
    }

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        if (!(element instanceof RDataType dt)) return;

        for (RAttribute attr : dt.attributes()) {
            if (attr.isOverride()) {
                checkOverride(dt, attr, collector);
            }
        }
    }

    private void checkOverride(RDataType dt, RAttribute attr, ValidationCollector collector) {
        ParentSearch search = findInherited(dt, attr.name());
        if (search.found() == null) {
            if (search.chainComplete()) {
                collector.error(
                    attr.sourceRange(),
                    "Attribute " + attr.name() + " does not exist in supertype",
                    ValidationIssueCode.MISSING_ATTRIBUTE);
            }
            return;
        }

        RMetaAnnotatedType overridden = typeEngine.getInferredAttributeType(attr);
        RMetaAnnotatedType parent = typeEngine.getInferredAttributeType(search.found());
        if (overridden.isMissing() || parent.isMissing()) {
            return;
        }
        if (!isSubtypeAtSeat(overridden.type(), parent.type())
                && !RBasicType.NOTHING.equals(parent.type())) {
            collector.error(
                attr.typeCall() != null ? attr.typeCall().sourceRange() : attr.sourceRange(),
                "The overridden type should be a subtype of the parent type "
                    + describe(parent.type()),
                ValidationIssueCode.TYPE_ERROR);
        }
    }

    /**
     * Upstream's subtype test at this seat ({@code treatChoiceTypesAsDataTypes=true}):
     * aliases unwrap first (upstream's alias arms preserve the flag), then a
     * choice on either side participates only as its DATA view — identity
     * admits; a data type whose extends-chain REACHES the parent choice admits
     * (Rune allows {@code type Foo extends ChoiceBar} — the fork's
     * {@code choiceSuperType()} channel; upstream's data-view walk hits the
     * same Choice object and admits by identity); {@code nothing} admits as
     * the bottom type and {@code any} as the top type; everything else
     * rejects — option-narrowing and sub-choice-narrowing (an option/sub-choice
     * does not EXTEND its choice, so upstream's data-view walk cannot reach
     * it: the documented #797 limitation), and a chain broken by an unresolved
     * extends link (upstream on the same shape resolves a null supertype and
     * rejects too). Non-choice shapes delegate to the shared relation, whose
     * remaining rules match upstream's.
     */
    private boolean isSubtypeAtSeat(RType sub, RType sup) {
        RType s = unalias(sub);
        RType p = unalias(sup);
        if (s instanceof RChoiceTypeRef || p instanceof RChoiceTypeRef) {
            if (s.equals(p)
                    || RBasicType.NOTHING.equals(s)
                    || RBasicType.ANY.equals(p)) {
                return true;
            }
            if (p instanceof RChoiceTypeRef choiceParent && choiceParent.astNode() != null
                    && s instanceof RDataTypeRef dataSub) {
                return chainReachesChoice(dataSub.astNode(), choiceParent.astNode());
            }
            return false;
        }
        return subtypeRelation.isSubtypeOf(sub, sup);
    }

    /**
     * Walks {@code start}'s extends-chain (both the data-supertype and the
     * choice-supertype channels) looking for {@code target} by node identity —
     * the fork twin of upstream's data-view supertype walk, which admits
     * {@code override attr Impl} where the parent declares choice {@code C}
     * and {@code Impl extends C} (the Seat-1 #442 MF-1 witness; cdm ships
     * four such extends-choice declarations).
     */
    private static boolean chainReachesChoice(RDataType start,
            com.regnosys.rosetta.ast.types.RChoice target) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        RDataType current = start;
        while (current != null && visited.add(current)) {
            if (current.choiceSuperType().orElse(null) == target) {
                return true;
            }
            current = current.superType().orElse(null);
        }
        return false;
    }

    private static RType unalias(RType t) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (t instanceof RAliasType a && visited.add(a)) {
            t = a.refersTo();
        }
        return t;
    }

    /**
     * Renders a type the way upstream's message concatenation does (upstream
     * concatenates the {@code RMetaAnnotatedType}, whose toString is the
     * RType's name-plus-present-parameters, suffixed {@code " with <meta>"}
     * when meta-annotated). Two fork representations are mapped back to
     * upstream's bytes: {@link RChoiceTypeRef#toString()} prepends
     * {@code "choice "}, so choices render via {@link RChoiceTypeRef#name()};
     * and the fork's {@code int} is a raw
     * {@code RNumberType(fractionalDigits: 0)} where upstream's is an
     * {@code RAliasType} named {@code int} whose parameter map has no
     * fractionalDigits entry — so integer number types render {@code int}
     * plus their OTHER present parameters.
     *
     * <p>Recorded corpus-invisible message faces (all ERROR-path-only, zero
     * corpus carriers): a meta-annotated parent renders WITHOUT upstream's
     * {@code " with <meta>"} suffix (the fork's attribute-type channel is
     * withNoMeta everywhere — the Seat-1 #442 MF-2 record); a parent declared
     * with inline builtin parameters ({@code int(digits: 5)},
     * {@code string(maxLength: 42)}) renders parameterless (the channel's
     * builtin fallback drops inline typeCall arguments — MF-3); a literal
     * {@code number(fractionalDigits: 0)} spelling renders {@code int}; and
     * user-declared alias names render alias-transparently.
     */
    private static String describe(RType t) {
        if (t instanceof RChoiceTypeRef c) {
            return c.name();
        }
        if (t instanceof RNumberType n && n.isInteger()) {
            StringBuilder sb = new StringBuilder("int");
            java.util.List<String> parts = new java.util.ArrayList<>();
            n.digits().ifPresent(d -> parts.add("digits: " + d));
            n.min().ifPresent(m -> parts.add("min: " + m));
            n.max().ifPresent(m -> parts.add("max: " + m));
            if (!parts.isEmpty()) {
                sb.append("(").append(String.join(", ", parts)).append(")");
            }
            return sb.toString();
        }
        return t.toString();
    }

    /** Nearest declaration of {@code name} in the ancestor chain, plus whether the chain resolved to its root. */
    private record ParentSearch(RAttribute found, boolean chainComplete) {}

    /**
     * Walks the resolved supertype chain looking for the nearest ancestor
     * declaration of {@code name} (upstream {@code RAttribute.getParentAttribute()}
     * semantics — an intermediate override shadows the original, so e.g.
     * upstream's {@code testCannotOverrideAttributeToDifferentSubtype} reports
     * the MIDDLE type's restriction as the parent type). The walk reports
     * {@code chainComplete=false} — and the caller declines — when it stops at
     * an unresolved {@code extends} link or a cyclic chain instead of a root.
     */
    private ParentSearch findInherited(RDataType dt, String name) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        RDataType current = dt;
        while (true) {
            if (!visited.add(current)) {
                return new ParentSearch(null, false);
            }
            RDataType parent = current.superType().orElse(null);
            if (parent == null) {
                // No extends clause anywhere on the node (neither the syntactic
                // name nor a resolved id) → the chain genuinely roots here and
                // absence is proven. An extends that failed to resolve → broken.
                boolean noExtendsAtAll = current.superTypeName().isEmpty()
                        && current.superTypeId().isEmpty();
                return new ParentSearch(null, noExtendsAtAll);
            }
            for (RAttribute a : parent.attributes()) {
                if (name.equals(a.name())) {
                    return new ParentSearch(a, true);
                }
            }
            current = parent;
        }
    }
}
