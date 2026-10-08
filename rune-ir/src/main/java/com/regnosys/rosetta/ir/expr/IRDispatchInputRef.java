package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A bare reference to a DISPATCH-BASE input from inside a per-enum-value dispatch VARIANT body
 * — the #505 {@code symbolUnresolved.synthetic.absent} teach (232 sole at the #504 SOT, the
 * cdm-twin 116/116 signature; the #504 qualNameGate census decoded the class — the
 * YearFraction-family overload bodies naming BASE-signature inputs — and the #505 dispatchGate
 * census proved it 100% {@code variant.baseInput}-hit, {@code [calculation]}-dominant): the
 * variant's own signature declares only the dispatch parameter (its {@code inputs()} is the
 * synthesized placeholder), so a body reference to a BASE declaration input resolves through
 * the dispatch scope-join legacy applies ({@code HandlerHelper.dispatchBaseOf} — the PR #369
 * facet threads the base as the signature source) and the fork's linker leaves EMPTY by
 * construction.
 *
 * <p><strong>Deliberately SHALLOW</strong> (the {@link IRSymbolNav} pattern): the input's name
 * ({@link #inputName()}) and its attribute-channel type are the neutral facts; the render —
 * the RAW name against the variant's evaluate signature (which carries the BASE's declared
 * inputs, the PR #369 signature law; the raw-name form is today's bytes — the
 * dispatch-variant escape-decline), the {@code [calculation]}-body coercions — is legacy's own
 * (the L-029 split). The DISTINCT kind is the safety (the #499 law): the Java emitter routes
 * every containing claim root through the compiler's oracle-root serve (the shared
 * {@code containsOracleLeaf} walk — the {@code navChain}/{@code bareSymbolRef} dispatch legs,
 * the literal {@code super.visitX} lines; the oracle-first design the #505 charter mandates
 * for the {@code [calculation]}-body byte risk), so the scope-join render is byte-identical BY
 * IDENTITY while this kind itself has NO leaf-emitter arm (a native compose can never reach it
 * — the routing safety, the #500 arm-A relocation).
 *
 * <p>Fork-authored (PR #505); not present upstream.
 */
public record IRDispatchInputRef(
        String inputName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.DISPATCH_INPUT_REF;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
