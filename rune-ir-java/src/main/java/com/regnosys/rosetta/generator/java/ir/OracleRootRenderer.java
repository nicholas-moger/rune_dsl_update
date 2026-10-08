package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.ir.expr.IRExpr;

/**
 * The #500 GENERIC root-site oracle renderer — ONE frame slot serving every claim root whose
 * render is the literal legacy fallback line, replacing what would otherwise be a per-kind slot
 * per family (the #494–#499 pattern's five slots stay untouched; this slot serves the NEW #500
 * root shapes):
 *
 * <ul>
 *   <li><strong>the arm-B family roots</strong> — {@code IRConversion} / {@code IRPipe} /
 *       {@code IROnlyExists} / a root-position {@code IRListConstruct} (the four untargeted-family
 *       teaches), rendered {@code super.visitConversion} / {@code super.visitThen} /
 *       {@code super.visitOnlyExists} / {@code super.visitListLiteral} — each the ONE-line legacy
 *       handler call ({@code conversionHandler} / {@code collectionHandler} /
 *       {@code existenceHandler} / {@code literalHandler}); and</li>
 *   <li><strong>the arm-A meta-bearing roots</strong> — a {@code FieldAccess} / {@code BinaryOp} /
 *       {@code Existence} / non-rule {@code IRApply} claim root whose lowered subtree contains an
 *       {@code IRMetaAccess} (the #499-exposed consumer faces), rendered
 *       {@code super.visitFeatureCall} / {@code super.visit<Equality|Comparison|Logical|Arithmetic>} /
 *       {@code super.visitExistence} / {@code super.visitSymbolReference} — the wrapper-typed
 *       {@code FieldWithMetaX} semantics (the {@code "Type coercion"} derefs the L-029 split keeps
 *       legacy-side) run inside legacy's own render.</li>
 * </ul>
 *
 * <p>The compiler ({@link IRExpressionCompiler}) installs the slot on the ROOT-SITE FAST PATH (the
 * #494 root-only law: no subtree pre-walk beyond the O(1)-screened meta-bearing check, no site
 * map): the builder captures the claim root's own raw node and dispatches ONE literal
 * {@code super.visitX} line by that node's family; the renderer serves ONLY on source-range
 * correlation ({@code Objects.equals} — null-tolerant, sound BY ROUTING exactly as the #498/#499
 * slots: the slot is installed only when the claim root IS a served shape, and at that root the
 * emitter whole-renders through this oracle BEFORE its walk could reach any interior node — an
 * interior served-shape node under any OTHER claim root finds a null or uncorrelated slot and
 * that claim declines at the emitter, the leafEmitter seat). Interior nodes re-enter the IR
 * compiler through the oracle's own recursion and claim at their own roots.
 *
 * <p>Fork-authored (PR #500); not present in the lab tree.
 */
@FunctionalInterface
public interface OracleRootRenderer {

    /**
     * The complete legacy render of this claim root, or {@code null} when the node cannot be
     * correlated to the captured claim-root site (→ the emitter declines to legacy).
     */
    JavaStatementBuilder render(IRExpr root);
}
