package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A bare reference to the enclosing function's declared OUTPUT from inside its own body —
 * the #514 {@code notAnInputParam.out} teach (the face read 100% output-by-identity at the
 * decode probe: {@code declaring=RFunction} with the bound attribute ≡ {@code fn.output()},
 * the {@code implicitAttrRoot} census's {@code noLambda.varPath} rows — legacy's ladder
 * variable-paths the output holder, the {@code MapperS.of(<outputName>)} render over the
 * function's own result local). The same class is the dominant HEAD family behind the
 * {@code inputFeatureNav.receiverNotExpressible.child.recv:notAnInputParam.out} disguised
 * navs ({@code identifiers -> observable} where {@code identifiers} is the output), so the
 * receiver admission heals both faces with one mint.
 *
 * <p><strong>Deliberately SHALLOW</strong> (the {@link IRDispatchInputRef} pattern): the
 * output's name ({@link #outputName()}) and its attribute-channel type are the neutral
 * facts; the render — the output-local naming, the builder-vs-value access forms, every
 * hop coercion off it — is legacy's own (the L-029 split). The DISTINCT kind is the safety
 * (the #499 law): no native consumer gate admits it silently, the #514 admissions name it,
 * and the Java emitter routes every containing claim root through the compiler's
 * oracle-root serve (the shared {@code containsOracleLeaf} walk — the literal
 * {@code super.visitX} lines), so the varPath render is byte-identical BY IDENTITY while
 * this kind itself has NO leaf-emitter arm (a native compose can never reach it — the
 * routing safety, the #500 arm-A relocation).
 *
 * <p>Fork-authored (PR #514); not present upstream.
 */
public record IROutputRef(
        String outputName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.OUTPUT_REF;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
