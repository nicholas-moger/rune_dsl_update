package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.external.RExternalClass;
import com.regnosys.rosetta.ast.external.RExternalRegularAttribute;
import com.regnosys.rosetta.ast.external.RExternalRuleSource;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.inference.CardinalityComputer;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.math.BigInteger;

/**
 * Validates report/rule-reference declarations (facet warningFamilyWaves,
 * PR #455).
 *
 * <p>Upstream: {@code ReportValidator} — two of its warning families land
 * here at the RELEASED-9.83.0 severities (the #454 severity oracle: the
 * released jar's bytecode, not the vendored tree):
 *
 * <ul>
 * <li>{@code checkRegulatoryReferenceAnnotation} — every doc reference
 *     spelled with the deprecated {@code regulatoryReference} keyword fires
 *     {@code Using `regulatoryReference` is deprecated. Use `docReference`
 *     instead} ({@code warningKeyword} in the released bytecode, anchored at
 *     the keyword — the drr bank's dominant class, 3,749 lines).</li>
 * <li>{@code checkOwnRuleReferenceAnnotations}' cardinality arm — a rule
 *     reference whose target attribute is single-cardinality but whose
 *     rule's output is multi fires {@code Expected single cardinality, but
 *     rule has multi cardinality} — the released bytecode emits
 *     {@code warning(...)} where the vendored source's {@code error} form is
 *     a post-release severity flip (the same axis as the #454
 *     {@code isSingleCheck} finding, bytecode-verified at
 *     {@code lambda$checkOwnRuleReferenceAnnotations$3}). Two entry seats,
 *     exactly upstream's: (a) {@code checkAttribute} — an attribute's OWN
 *     inline annotations, one pass per attribute; (b)
 *     {@code checkExternalRuleSource} — an external rule source's
 *     annotations, one pass PER REGULAR-ATTRIBUTE ROW (upstream iterates
 *     {@code getRegularAttributes()} and recomputes the same rule map for
 *     every row naming the attribute, so a {@code - x} remove row followed
 *     by a {@code + x [ruleReference ...]} add row fires the SAME
 *     annotation twice — the drr bank's hkma/mas double rows witness the
 *     multiplicity). The drr bank carries 14 lines. A path-carrying
 *     ({@code for a -> b}) annotation declines — every bank line is the
 *     path-less form, and an upstream fire on a path form would carry the
 *     path INSERTED MID-message (the released recipe is {@code Expected
 *     single cardinality<path>, but rule has multi cardinality}); none do.
 *     Multiplicity corner (Seat-1 #455 OBS): upstream fires rows × the
 *     path-keyed MERGED rule map (one result per path, anchored at the
 *     winning annotation) where the fork fires rows × all same-name-row
 *     annotations — identical while path-less annotations-per-attribute
 *     ≤ 1 per source, which the census pins corpus-wide; a source carrying
 *     two path-less annotations on one attribute would over-fire
 *     (corpus-empty, a corpus-bump watch item).</li>
 * </ul>
 *
 * <p>The remaining upstream ReportValidator arms (report input-type meets,
 * eligibility-rule kind/input errors, the explicitly-empty remove checks,
 * the rule-vs-target TYPE error) are ERROR-side and corpus-zero (the V0
 * oracle streams carry zero errors) — unlanded, recorded follow-ons.
 */
public final class ReportValidator implements Validator {

    private final CardinalityComputer cardinalityComputer;

    public ReportValidator(CardinalityComputer cardinalityComputer) {
        this.cardinalityComputer = cardinalityComputer;
    }

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        // The regulatoryReference deprecation — every doc reference under any
        // root element (attributes, types, enums, functions, rules,
        // conditions, external rows); the AST records the keyword spelling.
        // Anchored at the keyword token (upstream sites the issue one past the
        // opening bracket — the mojo-seam stream measured the fork's node
        // anchor one column early on all 3,749 drr bank lines; PR #458).
        for (RDocReference docRef : AstWalker.findAll(element, RDocReference.class)) {
            if (docRef.isRegulatoryReference()) {
                collector.warning(
                    docRef.tokenRanges().getOrDefault("keyword", docRef.sourceRange()),
                    "Using `regulatoryReference` is deprecated. Use `docReference` instead",
                    ValidationIssueCode.DEPRECATION);
            }
        }

        // Seat (a): inline rule references on a data type's own attributes.
        if (element instanceof RDataType dt) {
            for (RAttribute attr : dt.attributes()) {
                for (RRuleReferenceAnnotation ann : attr.ruleReferenceAnnotations()) {
                    checkRuleReferenceCardinality(ann, attr, collector);
                }
            }
        }

        // Seat (b): an external rule source's own annotations — one pass per
        // regular-attribute ROW, mirroring upstream's per-row recompute (the
        // fire multiplicity contract; see the class javadoc). Rows are
        // pre-grouped by name once so each row consults only its same-name
        // group (Copilot #455 R1 — the multiplicity is unchanged: per row ×
        // the group's annotations).
        if (element instanceof RExternalRuleSource source) {
            for (RExternalClass extClass : source.classes()) {
                java.util.Map<String, java.util.List<RExternalRegularAttribute>> rowsByName =
                        new java.util.LinkedHashMap<>();
                for (RExternalRegularAttribute row : extClass.attributes()) {
                    rowsByName.computeIfAbsent(row.name(), k -> new java.util.ArrayList<>())
                            .add(row);
                }
                for (RExternalRegularAttribute row : extClass.attributes()) {
                    RAttribute attr = row.resolvedAttribute().orElse(null);
                    if (attr == null) {
                        continue;
                    }
                    for (RExternalRegularAttribute annRow
                            : rowsByName.getOrDefault(row.name(), java.util.List.of())) {
                        for (RRuleReferenceAnnotation ann : annRow.ruleRefs()) {
                            checkRuleReferenceCardinality(ann, attr, collector);
                        }
                    }
                }
            }
        }
    }

    /**
     * The cardinality arm: target single + rule output multi → the released
     * WARNING, anchored at the annotation (upstream anchors
     * {@code ruleResult.getOrigin()} = the same annotation node). The rule's
     * output cardinality is its body expression's — upstream
     * {@code buildRFunction(rule).getOutput().isMulti()} reads the rule
     * expression through {@code CardinalityProvider}; the fork's faithful
     * entry is {@code computeRuleBody} (see ExpressionValidator's
     * checkEqualityCardinality note).
     */
    private void checkRuleReferenceCardinality(RRuleReferenceAnnotation ann, RAttribute target,
            ValidationCollector collector) {
        if (ann.forPath().isPresent()) {
            return;
        }
        RRule rule = ann.rule().orElse(null);
        if (rule == null || rule.expression().isEmpty()) {
            return;
        }
        if (isMulti(target)) {
            return;
        }
        if (cardinalityComputer.computeRuleBody(rule.expression().get())
                == ExpressionCardinality.MULTI) {
            collector.warning(
                ann.sourceRange(),
                "Expected single cardinality, but rule has multi cardinality",
                ValidationIssueCode.CARDINALITY_ERROR);
        }
    }

    private static boolean isMulti(RAttribute attr) {
        RCardinality card = attr.cardinality().orElse(null);
        if (card == null) {
            return false;
        }
        return card.isUnbounded()
                || (card.sup() != null && card.sup().compareTo(BigInteger.ONE) > 0);
    }
}
