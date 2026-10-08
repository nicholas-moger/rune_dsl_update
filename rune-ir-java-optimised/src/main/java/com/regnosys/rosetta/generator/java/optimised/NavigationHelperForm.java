package com.regnosys.rosetta.generator.java.optimised;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * THE LOOP-FORM HELPER BUILDER — the census § 11 design of record, built,
 * GATE-PROVEN EQUIVALENT (rune-equivalence 22/0/0/0 ×2 on the loop-bearing
 * overlay; O5 zero — private members are API-invisible) and then MEASURED OUT
 * (§ 11: the same-day 6-fork A/B priced the loop-bearing DRR tree
 * <b>+494.7 MB/op ABOVE</b> the landed § 10b tree, fully disjoint — beside the
 * § 9d stream pipeline's ~+405). <b>No live emitter path calls this class</b> —
 * the {@code STREAM} arm stays the § 9d/§ 11 decline; the shared mechanism (the
 * boundary contract's MATERIALIZED flattened List escapes into the long-lived
 * {@code MapperC}, where the reference chain's intermediate items die young under
 * escape analysis) means a re-price re-enters only with a CHANGED boundary
 * contract, not another spelling of this form.
 *
 * <p>The form: renders a {@code STREAM}-shape
 * {@link NavigationChainClassifier.LadderPlan} as the BODY of a generated
 * {@code private static} per-file helper method — plain nested index loops with
 * ZERO stream machinery; the per-call allocation is ONE
 * {@code java.util.ArrayList} — the § 9a collector's own terminal allocation,
 * with everything upstream of it deleted.
 *
 * <p><b>The staging mirrors the § 9a pipeline exactly</b> (the null/error
 * equivalence ledger, census § 9): the BASE (the multi root, or the ladder up to and
 * INCLUDING the first multi accessor) null → the helper returns {@code null} (the
 * boundary {@code MapperC.of(null)} — identical to the stream form's guarded base);
 * each consecutive-single run is ONE mini-ladder over the stage's element local
 * (terminal nulls KEPT — the boundary drop is the reference error-item drop); each
 * SUBSEQUENT multi hop binds its run's child list and loops it under a null guard
 * (the {@code flatMap}-empty twin). Iteration is index-based in List encounter
 * order — the {@code stream()} traversal order.
 *
 * <p><b>Spelling posture:</b> every type inside the helper is FQN-INLINE
 * ({@code java.util.List}/{@code java.util.ArrayList} + the element classes at the
 * multi seats) — ZERO import-machinery interaction, the § 9a FQN posture carried
 * over; the CALL-site boundary wrap keeps the § 9a witness + refs belt unchanged.
 * Locals are {@code _nav<i>} (the corpus-verified collision-free family) plus
 * {@code _navResult}; the method name ({@code _navStream<i>}) is assigned by the
 * per-file registry ({@link OptimisedExpressionCompiler}) AFTER text-keyed dedup —
 * the {@link HelperText} pieces deliberately exclude it.
 */
final class NavigationHelperForm {

    private NavigationHelperForm() {
    }

    /**
     * One helper's name-independent text: the return declaration
     * ({@code java.util.List<RET>}), the parameter declaration
     * ({@code TYPE name}), and the body lines (RELATIVE indentation — leading
     * {@code \t}s only; the drain prefixes the host class's member indent). The
     * dedup key is the concatenation of all three — identical text is identical
     * semantics, so two sites sharing a chain share one helper.
     */
    record HelperText(String returnDecl, String paramDecl, List<String> bodyLines) {

        String dedupKey() {
            return returnDecl + '|' + paramDecl + '|' + String.join("\n", bodyLines);
        }
    }

    /**
     * Builds the loop-form helper text for a {@code STREAM}-shape plan.
     *
     * @param plan the classified chain (shape must be {@code STREAM})
     * @param returnElementFqn the boundary element's canonical name — the § 5a
     *        witness type (the wrapper class for a meta top), FQN-spelled
     * @param rootElementFqn the PARAM root's canonical element name (the loop
     *        element for a multi root; the parameter type for a single root)
     * @param multiStepElementFqn canonical element name at accessor step {@code i}
     *        (consulted ONLY at multi steps — the wrapper class for a meta getter
     *        step, per the § 10a factory channel)
     */
    static HelperText build(NavigationChainClassifier.LadderPlan plan,
            String returnElementFqn, String rootElementFqn,
            IntFunction<String> multiStepElementFqn) {
        if (plan.shape() != NavigationChainClassifier.LadderPlan.Shape.STREAM) {
            throw new IllegalArgumentException(
                    "the loop helper form is defined for the STREAM shape only; this plan is "
                            + plan.shape());
        }
        String returnDecl = "java.util.List<" + returnElementFqn + ">";
        List<String> lines = new ArrayList<>();
        int[] navCounter = {0};
        String loopList;
        String loopElemFqn;
        int nextHop;
        String paramDecl;
        if (plan.rootMulti()) {
            // The multi root IS the base — the stream form's `rootName.stream()`.
            paramDecl = "java.util.List<? extends " + rootElementFqn + "> " + plan.rootName();
            lines.add("if (" + plan.rootName() + " == null) {");
            lines.add("\treturn null;");
            lines.add("}");
            loopList = plan.rootName();
            loopElemFqn = rootElementFqn;
            nextHop = 0;
        } else {
            // Single root, interior multi: the base is the ladder up to and
            // INCLUDING the first multi accessor (§ 9a), bound once.
            paramDecl = rootElementFqn + " " + plan.rootName();
            int firstMulti = plan.hopMulti().indexOf(true);
            String baseVar = "_nav" + navCounter[0]++;
            lines.add("java.util.List<? extends " + multiStepElementFqn.apply(firstMulti)
                    + "> " + baseVar + " = "
                    + plan.ladderOver(plan.rootName(), 0, firstMulti + 1) + ";");
            lines.add("if (" + baseVar + " == null) {");
            lines.add("\treturn null;");
            lines.add("}");
            loopList = baseVar;
            loopElemFqn = multiStepElementFqn.apply(firstMulti);
            nextHop = firstMulti + 1;
        }
        lines.add(returnDecl + " _navResult = new java.util.ArrayList<>();");
        emitLoop(plan, lines, navCounter, loopList, loopElemFqn, nextHop, 0,
                multiStepElementFqn);
        lines.add("return _navResult;");
        return new HelperText(returnDecl, paramDecl, lines);
    }

    /**
     * One loop level over {@code listVar} (elements {@code elemFqn}): binds the
     * element local, then either recurses through the next multi hop's child list
     * (null-guarded — the {@code flatMap}-empty twin) or adds the final run's
     * mini-ladder value ({@code _navResult.add(elem)} when the run is empty — the
     * flattened-element-is-the-value stage).
     */
    private static void emitLoop(NavigationChainClassifier.LadderPlan plan,
            List<String> lines, int[] navCounter, String listVar, String elemFqn,
            int nextHop, int depth, IntFunction<String> multiStepElementFqn) {
        String ind = "\t".repeat(depth);
        String idxVar = "_nav" + navCounter[0]++;
        String elemVar = "_nav" + navCounter[0]++;
        lines.add(ind + "for (int " + idxVar + " = 0; " + idxVar + " < " + listVar
                + ".size(); " + idxVar + "++) {");
        lines.add(ind + "\t" + elemFqn + " " + elemVar + " = " + listVar + ".get("
                + idxVar + ");");
        int accessorCount = plan.accessors().size();
        int i = nextHop;
        while (i < accessorCount && !plan.hopMulti().get(i)) {
            i++;
        }
        if (i < accessorCount) {
            // Steps nextHop..i-1 are single, step i is MULTI: bind the run's child
            // list, loop it under the null guard.
            String childVar = "_nav" + navCounter[0]++;
            lines.add(ind + "\tjava.util.List<? extends " + multiStepElementFqn.apply(i)
                    + "> " + childVar + " = " + plan.ladderOver(elemVar, nextHop, i + 1)
                    + ";");
            lines.add(ind + "\tif (" + childVar + " != null) {");
            emitLoop(plan, lines, navCounter, childVar, multiStepElementFqn.apply(i),
                    i + 1, depth + 2, multiStepElementFqn);
            lines.add(ind + "\t}");
        } else {
            lines.add(ind + "\t_navResult.add("
                    + plan.ladderOver(elemVar, nextHop, accessorCount) + ");");
        }
        lines.add(ind + "}");
    }
}
