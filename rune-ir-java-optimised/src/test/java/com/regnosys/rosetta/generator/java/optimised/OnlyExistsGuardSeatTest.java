package com.regnosys.rosetta.generator.java.optimised;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * v3.2 seat 8 — the optimised route's only-exists guard, re-pinned to the STRUCTURE after the seat's two
 * catches. {@link NavigationChainClassifier#isMaximalChainTopCandidate} must never admit a chain that sits
 * anywhere inside an {@code only exists} path element — the classifier's own law ("the classifier refuses
 * them REGARDLESS — defense in depth"): the deprecated list form of {@code ExpressionOperators.onlyExists}
 * reads the mappers' parent items and paths, the 3-arg form the generators render reads {@code getMulti}
 * alone, and the exemption holds whichever form a consumer reads. Until seat 8 the guard read the chain's
 * PARENT alone, which was blind in two directions:
 *
 * <ul>
 *   <li><b>The cdm catch (+8).</b> The seat made the element's leaf a real {@link RFeatureCall} (F15: the
 *       path {@code t -> p} is one expression, the leaf the element's child and the receiver the leaf's), so the
 *       generator's synthesized item-rooted receiver twin — parented where the root reference sits, under the
 *       LEAF now — stopped matching the parent test: eight cdm 5.38.0 receivers
 *       ({@code then primitiveInstruction -> split only exists} and its kin) became SINGLE item ladder
 *       candidates, and the emission suite's conversion-event pin caught it (782 → 790 on {@code cdm/5.38.0},
 *       {@code itemBySteps 1: 21 → 29} — the FIRST pin to fail; the figure re-taken at the round-1 head with the
 *       classifier's read reverted to the parent test by text, the print preserved at
 *       {@code target/v32-seat8-instruments/scratch/plus8-r1.log} and the sites named by the per-site diff against
 *       run B ({@code sites-diff-B-G1.txt}, local): the class reaches 94 twins under the leaf over 20 of the 21
 *       function-bearing cells — cdm 5.38.0 / 5.39.0 eight each, the eight cdm 6.x cells four each, drr/5.61.0 ten,
 *       every drr 6.x / 7.x cell four, chaos zero — beside the 34 root twins below; the seat had named cdm/5.38.0's
 *       eight alone, the pin's own print) before any emission shipped.</li>
 *   <li><b>The drr catch (−34).</b> The same twin of a two-or-more-hop path ({@code terminationProvision ->
 *       earlyTerminationProvision -> optionalEarlyTermination only exists} inside a lambda) hangs off the hop
 *       ABOVE the root — one level below the element — and the parent test had ADMITTED it since the ladder
 *       landed: 34 conversions over the ten drr cells at the committed head (drr/5.61.0 seven, every drr 6.x
 *       and 7.x cell three; none on any cdm cell or the chaos cell), named site by site by the seat's per-site
 *       instrument ({@code -Doptnav.dump-sites}, two contents diffed). The ancestor walk refuses them and the
 *       conversion-event pins moved with it (6,218 → 6,211 on {@code drr/5.61.0}).</li>
 * </ul>
 *
 * <p>The guard consults {@link ROnlyExistsElement#encloses} now — the ONE enclosure read (LAW 69) the
 * resolver's diagnostic exemption, the IR adapter's decode and the alias-seam policy consult too. The
 * witnesses build the seat's real leaf shape from source and the generator's twin by hand at the exact
 * parenting the generator uses ({@code setParent(root.parent())}); the control is the same chain OUTSIDE an
 * only-exists element, which the classifier must keep admitting.
 *
 * <p><b>THE LANE</b> (lane G1 of {@code target/v32-seat8-instruments/lanes-s8.py}, measured at the seat-suite head
 * {@code 6d3737e2d} — {@code scratch/lanes-s8-c6.status}, local — and re-taken WHOLE at the round-1 code head
 * {@code 688d78539} ({@code scratch/lanes-s8-c10.status}) and the round-2 code head {@code 22f71e1c8}
 * ({@code scratch/lanes-s8-c12.status}), the set UNMOVED — the pinned per-run copies, never the rolling file; LAW 82): the
 * classifier's enclosure read reverted to
 * the PARENT test → {@link #theTwinUnderTheLeaf_isRefused_theCdmCatch} and
 * {@link #theRootTwinOneHopBelowTheLeaf_isRefused_theDrrCatch} red; the structure test, the control and (since round 1)
 * {@link #theInstrumentsOwnWalk_agreesWithTheOneRead_atEveryNode} green — EXACTLY the two catches, nothing else. The corpus witness of the same mutation is the emission suite's conversion-event pin
 * (cdm/5.38.0 782 → 790 under the parent test at the F15 content; the drr rows −34 under the walk), measured by the
 * two-content per-site run rather than by a lane.
 */
class OnlyExistsGuardSeatTest {

    private static final String MODEL = """
            namespace test
            type T:
                p string (0..1)
                a A (0..1)
            type A:
                b string (0..1)
            func G:
                inputs:
                    t T (1..1)
                    ts T (0..*)
                output:
                    ok boolean (0..*)
                add ok: t -> p only exists
                add ok: t -> a -> b only exists
                add ok: ts extract [ p only exists ]
                add ok: t -> a -> b exists
            """;

    private static RModel linked() {
        RModel model = AstBuilder.buildFromString(MODEL, "OnlyExistsGuardSeatTest.rosetta");
        RWorkspace.build(List.of(model));
        return model;
    }

    private static ROnlyExistsElement pathed(RModel model, List<String> chain) {
        return AstWalker.findAll(model, ROnlyExistsElement.class).stream()
                .filter(e -> e.leafReference() != null && e.featureChain().equals(chain))
                .findFirst().orElseThrow();
    }

    /** The generator's twin, at its exact parenting ({@code ReferenceHandler.synthesizeImplicitItemBareNav}). */
    private static RFeatureCall twinOf(RSymbolReference root) {
        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(root.parent());
        RFeatureCall twin = new RFeatureCall();
        twin.setReceiver(item);
        twin.setFeatureName(root.name());
        twin.setParent(root.parent());
        return twin;
    }

    @Test
    void theLeafAndEveryHopUnderAnOnlyExistsElement_areNeverChainTopCandidates() {
        RModel model = linked();
        List<ROnlyExistsElement> elements = AstWalker.findAll(model, ROnlyExistsElement.class);
        // the two named-root elements carry a synthesized path; the bare `p only exists` inside the
        // lambda is a declined edge shape (no path, banked) and contributes no chain
        List<ROnlyExistsElement> pathed = elements.stream().filter(e -> e.leafReference() != null).toList();
        assertEquals(2, pathed.size(), "two pathed elements");
        for (ROnlyExistsElement el : pathed) {
            RFeatureCall leaf = el.leafReference();
            assertSame(el, leaf.parent(), "the leaf is the element's child");
            assertTrue(ROnlyExistsElement.encloses(leaf));
            assertFalse(NavigationChainClassifier.isMaximalChainTopCandidate(leaf), "the leaf is never a candidate");
            if (el.receiverExpression() instanceof RFeatureCall hop) {
                assertSame(leaf, hop.parent(), "the receiver hop hangs off the leaf, no longer off the element");
                assertSame(hop, leaf.receiver());
                // a receiver hop is the leaf's RECEIVER: the interior rule refuses it with or without the
                // enclosure read — stated as the structural fact it is, not claimed as a guard witness
                assertFalse(NavigationChainClassifier.isMaximalChainTopCandidate(hop));
            }
        }
    }

    @Test
    void theTwinUnderTheLeaf_isRefused_theCdmCatch() {
        RModel model = linked();
        ROnlyExistsElement oneHop = pathed(model, List.of("p"));
        RSymbolReference root = (RSymbolReference) oneHop.receiverExpression();
        RFeatureCall twin = twinOf(root);
        assertSame(oneHop.leafReference(), twin.parent(), "the twin's parent is the LEAF, not the element");
        assertFalse(twin.parent() instanceof ROnlyExistsElement, "…so the pre-seat PARENT test would have admitted it");
        assertTrue(ROnlyExistsElement.encloses(twin), "the enclosure read sees the element above the leaf");
        assertFalse(NavigationChainClassifier.isMaximalChainTopCandidate(twin),
                "the +8 class: a twin under the leaf is refused (782 -> 790 conversion events on cdm/5.38.0 before the walk; 94 such twins over 20 cells at round 1's re-measure)");
    }

    @Test
    void theRootTwinOneHopBelowTheLeaf_isRefused_theDrrCatch() {
        RModel model = linked();
        ROnlyExistsElement twoHop = pathed(model, List.of("a", "b"));
        RFeatureCall hopA = (RFeatureCall) twoHop.receiverExpression();
        RSymbolReference root = (RSymbolReference) hopA.receiver();
        RFeatureCall twin = twinOf(root);
        assertSame(hopA, twin.parent(), "the root twin hangs off the hop ABOVE the root — one level below the element");
        assertNotSame(twoHop, twin.parent());
        assertNotSame(twoHop.leafReference(), twin.parent());
        assertFalse(twin.parent() instanceof ROnlyExistsElement,
                "the pre-seat PARENT test admitted this twin — the 34 drr conversions the per-site instrument named");
        assertTrue(ROnlyExistsElement.encloses(twin), "the enclosure read walks past the hop to the element");
        assertFalse(NavigationChainClassifier.isMaximalChainTopCandidate(twin),
                "the -34 class: the root twin of a two-or-more-hop only-exists path is refused (6,218 -> 6,211 on drr/5.61.0)");
    }

    /**
     * The per-site instrument's own ancestor walk ({@code OptimisedNavigationEmissionTest.enclosedByOnlyExists},
     * written out so a dump can be taken at a content without the declaration) must agree with the ONE read at
     * every node — the fixture's whole tree, twins included (round 1, the code-quality NIT-9).
     */
    @Test
    void theInstrumentsOwnWalk_agreesWithTheOneRead_atEveryNode() {
        RModel model = linked();
        List<RNode> nodes = AstWalker.findAll(model, RNode.class);
        assertTrue(nodes.size() > 20, "the fixture parses to a real tree");
        int enclosed = 0;
        for (RNode node : nodes) {
            boolean one = ROnlyExistsElement.encloses(node);
            assertEquals(one, OptimisedNavigationEmissionTest.enclosedByOnlyExists(node),
                    "the instrument's walk disagrees with encloses at " + node.getClass().getSimpleName());
            if (one) {
                enclosed++;
            }
        }
        // the two pathed elements' whole synthesized paths: `t -> p` (root + leaf) and `t -> a -> b` (root + hop + leaf)
        assertEquals(5, enclosed, "five nodes sit inside an only-exists element in the fixture");
        // the generator's twins too, at their exact parenting: the root twin of EVERY pathed element — under the leaf
        // for `t -> p` (the +8 class), under the hop above the root for `t -> a -> b` (the -34 class)
        int twins = 0;
        for (ROnlyExistsElement el : AstWalker.findAll(model, ROnlyExistsElement.class)) {
            RNode down = el.leafReference();
            while (down instanceof RFeatureCall fc) {
                down = fc.receiver();
            }
            if (down instanceof RSymbolReference root) {
                RFeatureCall twin = twinOf(root);
                assertTrue(ROnlyExistsElement.encloses(twin));
                assertEquals(ROnlyExistsElement.encloses(twin), OptimisedNavigationEmissionTest.enclosedByOnlyExists(twin));
                twins++;
            }
        }
        assertEquals(2, twins, "both pathed elements' root twins compared");
    }

    @Test
    void control_theSameChainOutsideAnOnlyExistsElement_staysACandidate() {
        RModel model = linked();
        // `t -> a -> b exists` — the same two-hop navigation, an ordinary existence operand
        RFeatureCall outside = AstWalker.findAll(model, RFeatureCall.class).stream()
                .filter(fc -> "b".equals(fc.featureName()) && !ROnlyExistsElement.encloses(fc)).findFirst().orElse(null);
        assertNotNull(outside, "the control chain parses as a feature call");
        assertTrue(NavigationChainClassifier.isMaximalChainTopCandidate(outside),
                "the guard is scoped to only-exists paths: an ordinary chain top is admitted as before");
        assertFalse(ROnlyExistsElement.encloses(outside));
    }
}
