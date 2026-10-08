package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * An input-feature navigation over an OUTPUT-consuming alias head ({@code <alias> ->
 * <feature>} where the alias body references the enclosing function's output — the
 * {@code aliasNavBodyFacet} {@code usesOutput} face) — the #518 teach at the shared
 * {@code aliasHeadNavLowering} core (both seats: the disguised {@code REnumValueRef}
 * channel and the parsed {@code RFeatureCall} mirror).
 *
 * <p><strong>Why a DISTINCT kind (the #499 law):</strong> legacy renders an
 * output-consuming alias nav as a BUILDER walk ({@code FunctionAliasHelper
 * .inferShortcutMapperJavaType} hard-declines the Mapper signature for an
 * output-referencing body), so the {@code FieldAccess}-over-{@code IRReference{ALIAS}}
 * shape — whose emitter render IS a Mapper compose — must never carry this face. The
 * distinct kind keeps every Mapper-shaped admission byte-frozen by construction: it has
 * no leaf-emitter arm, it joins {@code containsOracleLeaf} / the {@code isOracleRootShape}
 * O(1) screens, and every claim root that carries it renders the literal legacy line BY
 * IDENTITY (the L-111 relabel seat's #507 identity-serve leg; the standing
 * enumChain/navChain oracle serves at the probed roots) — legacy's own builder-walk
 * machinery stays inside the serve (the L-029 split).
 *
 * <p><strong>Deliberately SHALLOW</strong> (the {@link IRRuleInputNav} pattern): only the
 * alias head name and the feature name are carried — no body lowering, no hop typing (the
 * type/cardinality channels are stamped as read from the node's own engine channels, the
 * #491 convention; nothing reads them at render, every route is whole-legacy).
 *
 * <p>Fork-authored (PR #518); not present upstream.
 */
public record IROutputAliasNav(
        String headName,
        String featureName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.OUTPUT_ALIAS_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
