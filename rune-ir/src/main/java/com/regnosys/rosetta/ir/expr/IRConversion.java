package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A typed conversion over a single argument subtree — {@code <child> to-enum <Target>} /
 * {@code to-number} / {@code to-int} / {@code to-date} / {@code to-time} / {@code to-date-time} /
 * {@code to-zoned-date-time} — the #500 arm-B teach of the {@code RConversionExpr} family (956
 * events at the #500 census, 954 of them {@code to-enum} with the argument lowering in 946). The
 * {@code to-string} form is the SEPARATE {@link IRToString} kind (its own established seat) and
 * never builds this node. The neutral facts carried: {@link #conversionKind()} (the raw
 * {@code ConversionKind} constant name — a neutral operator token, no rune-AST enum imported) and
 * {@link #targetTypeName()} (the {@code to-enum} target's name, {@code null} for the non-enum
 * kinds); the single {@link #children() child} is load-bearing and recurses through the adapter
 * (all-or-nothing — a non-lowerable argument declines the node, the census's ~10-event residue).
 *
 * <p>At the Java target the ENTIRE render — the {@code to-enum} string/enum-source dispatch, the
 * {@code Optional}-chained {@code valueOf} facet family and every numeric/temporal parse form —
 * is a <strong>Java-emission decision</strong> kept off this neutral node (the L-029 split): the
 * emitter delegates to the compiler's range-correlated oracle-root renderer, which calls
 * {@code super.visitConversion(site, ctx)} on the source-range-correlated raw node — the LITERAL
 * legacy fallback call ({@code ExpressionCompiler.visitConversion} is one line:
 * {@code conversionHandler.handle(expr, ctx, this)}), same method, same handler instance, same
 * arguments, so the render is byte-identical to the decline path by the strongest argument. A
 * future Python/Rust emitter renders from {@code child} + the kind/target facts directly.
 *
 * <p>Fork-authored (PR #500); not present upstream.
 */
public record IRConversion(
        IRExpr child,
        String conversionKind,
        String targetTypeName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.CONVERSION;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(child);
    }
}
