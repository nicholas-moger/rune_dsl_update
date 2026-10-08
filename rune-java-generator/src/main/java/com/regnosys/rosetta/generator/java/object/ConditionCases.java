package com.regnosys.rosetta.generator.java.object;

import java.util.ArrayList;
import java.util.List;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RTypeAlias;

/**
 * v3.2 seat 3 (census family F9), the round-1 code-quality review (N-4): the condition-class
 * NAMING LAWS shared by every consumer that names a condition class from its owner — the
 * data-rule generator, which EMITS the class, and the type-format validator's alias-condition
 * wing, which {@code @Inject}s it — in one neutral home, so neither generator depends on the
 * other's class (LAW 69: the two halves consult one declaration).
 *
 * <p>Upstream: {@code JavaConditionInterface.create / computeConditionClassName} — the class is
 * {@code <Owner><Condition>} whatever the owner kind ({@code Data}, {@code choice},
 * {@code typeAlias}); an unnamed condition takes the count-all-named suffix of
 * {@link #casesOf}.
 */
public final class ConditionCases {

    private ConditionCases() {
    }

    /**
     * One condition class: a declaring type + its condition ({@code null} for a {@code choice}
     * type's synthesized {@code <Name>Choice} one-of) + the resolved condition class-name suffix.
     */
    public record ConditionCase(RRootElement declaringType, RCondition condition, String conditionName) {
    }

    /**
     * The per-type condition cases — the datarule twin of
     * {@code ModelMetaGenerator.collectConditionRefs}'s per-link body (which is byte-proven
     * corpus-wide through the XMeta {@code dataRules} refs): a {@code choice} contributes exactly
     * its implicit {@code Choice} case; a {@code Data} — and, since v3.2 seat 3, a
     * {@code typeAlias} (upstream {@code computeConditionClassName} reads
     * {@code cond.getEnclosingType()}, Data or alias alike) — contributes one case per OWN
     * condition in declaration order, unnamed conditions named by the count-all-named law.
     *
     * <p><b>The count-all-named law is upstream's own, ported faithfully</b> (upstream
     * {@code JavaConditionInterface.computeConditionClassName}:
     * {@code filter(named).takeWhile(c != cond).count()} — an UNNAMED condition never appears
     * in the name-filtered stream, so {@code takeWhile} consumes it whole and EVERY unnamed
     * condition gets the SAME total-named-count suffix). Two-plus unnamed conditions in one
     * type would therefore collide in upstream exactly as here — a corpus-fitted invariant,
     * NOT a fork choice: max-unnamed-per-type = 1 at 9.83.0 (Seat-1-recensused at PR #407;
     * the standing forward-note lives on {@link ModelMetaGenerator#unnamedConditionKind} —
     * re-check at any corpus/version bump). Diverging to per-unnamed increments would break
     * byte-identity with the golden class names AND the XMeta refs.
     */
    public static List<ConditionCase> casesOf(RRootElement element) {
        if (element instanceof RChoice choice) {
            return List.of(new ConditionCase(choice, null, "Choice"));
        }
        List<RCondition> conditions = element instanceof RTypeAlias alias
                ? alias.conditions()
                : ((RDataType) element).conditions();
        long namedCount = conditions.stream()
                .filter(c -> c.name().isPresent())
                .count();
        List<ConditionCase> cases = new ArrayList<>();
        for (RCondition condition : conditions) {
            String conditionName = condition.name()
                    .orElseGet(() -> ModelMetaGenerator.unnamedConditionKind(condition) + namedCount);
            cases.add(new ConditionCase(element, condition, conditionName));
        }
        return cases;
    }

    /**
     * The Rune name of a condition OWNER ({@code Data}, {@code choice} or {@code typeAlias}) —
     * the condition class-name prefix and the datarule package key (upstream
     * {@code JavaConditionInterface.create} reads {@code cond.getEnclosingType()}).
     */
    public static String ownerName(RRootElement element) {
        if (element instanceof RDataType dataType) {
            return dataType.name();
        }
        if (element instanceof RTypeAlias alias) {
            return alias.name();
        }
        return ((RChoice) element).name();
    }

    /**
     * v3.2 seat 3 (F9): the fail-closed gate on the parameterised sub-wing — the alias's first
     * type-parameter name when {@code alias} is PARAMETERISED, else {@code null}. The gate keys
     * on the OWNER, not on whether a condition's expression spells a parameter: at 9.83.0 there
     * is no parameters wing at all, so a parameter name in a condition body is not even a
     * symbol — both parsers read {@code item <= max} as the {@code max} LIST OP over the item
     * (upstream renders {@code MapperC.of(Collections.singletonList(bounded)).max()}, the fork
     * {@code MapperS.of(bounded)}; oracle group alias-conditions-param, banked) — and a
     * parameter-free condition on a parameterised alias has no corpus carrier to justify either
     * render. Refusing the owner's classes whole keeps the shape loud until a carrier appears.
     * (Round-1 cq SF-8: the former name {@code parameterReadByCondition(alias, condition)}
     * promised an inspection of the condition body it never made.)
     */
    public static String parameterisedOwner(RTypeAlias alias) {
        if (alias.typeParameters().isEmpty()) {
            return null;
        }
        RTypeParameter first = alias.typeParameters().get(0);
        return first.name() == null ? "?" : first.name();
    }
}
