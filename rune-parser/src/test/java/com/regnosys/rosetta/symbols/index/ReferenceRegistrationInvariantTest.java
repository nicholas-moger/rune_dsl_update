package com.regnosys.rosetta.symbols.index;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.testutil.CorpusCatalogue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE REVERSE INDEX HOLDS EACH REFERRER ONCE (v3.1 C1, Copilot R8 on PR #566).
 *
 * <p>{@link ReferenceIndex#registerReference} used to append unconditionally,
 * so a source that registered against the same target on more than one pass
 * appeared that many times in {@link RWorkspace#findReferences} — the index
 * that backs find-references, silently over-reporting.
 *
 * <p><b>How it was reached, and why the choice-option work exposed it.</b>
 * {@code TypeInferenceEngine}'s Category-1 arm was gated on
 * {@code fc.resolvedFeature().isEmpty()}, while
 * {@code TypeDirectedResolver.resolveFeatureCall} fills {@code resolvedFeature}
 * only for an {@code RAttribute} — the legacy slot is typed to {@code RAttribute}
 * and structurally cannot hold a choice option. So a feature call binding an
 * OPTION or a META type set {@code resolvedFeatureNode} but never
 * {@code resolvedFeature}: its gate never closed, and every fixed-point
 * iteration re-ran the lookup and re-registered the reference. Attribute seats
 * were unaffected because their gate does close — which is exactly the
 * asymmetry {@link #attribute_seats_are_the_control} pins.
 *
 * <p>Both halves are locked here: the engine no longer re-resolves a bound
 * feature call, and the index refuses a duplicate even if some future caller
 * does.
 */
class ReferenceRegistrationInvariantTest {

    /**
     * The unit invariant — the index itself deduplicates, by IDENTITY, which is
     * the semantics its {@code IdentityHashMap} keying already implies.
     */
    @Test void index_registers_a_given_source_once_per_target() {
        ReferenceIndex index = new ReferenceIndex();
        RNode target = new StubNode();
        RNode a = new StubNode();
        RNode b = new StubNode();

        index.registerReference(a, target);
        index.registerReference(a, target);
        index.registerReference(a, target);
        index.registerReference(b, target);

        List<RNode> refs = index.findReferences(target);
        assertEquals(2, refs.size(), "a source registering repeatedly must appear once");
        assertTrue(refs.contains(a));
        assertTrue(refs.contains(b));
        assertEquals(a, refs.get(0), "first-registration order is preserved");
    }

    /**
     * The engine invariant, over the real conformance suite: NO feature call is
     * registered more than once against the feature it resolved to.
     */
    @Test void no_feature_call_is_registered_twice() throws IOException {
        RWorkspace ws = buildConformanceWorkspace();

        List<String> offenders = new ArrayList<>();
        int seats = 0;
        for (RFeatureCall fc : featureCalls(ws)) {
            if (fc.resolvedFeatureNode().isEmpty()) continue;
            seats++;
            RNode target = fc.resolvedFeatureNode().get();
            long occurrences = ws.findReferences(target).stream().filter(r -> r == fc).count();
            if (occurrences > 1) {
                offenders.add(fc.featureName() + " -> " + target.getClass().getSimpleName()
                        + " registered " + occurrences + "x");
            }
        }

        assertTrue(seats > 0, "the suite must contain resolved feature calls, or this proves nothing");
        assertTrue(offenders.isEmpty(),
                "feature calls registered more than once in the reverse index:\n  "
                        + String.join("\n  ", offenders));
    }

    /**
     * THE POSITIVE CONTROL for the test above. Attribute-resolving seats close
     * their gate and were never duplicated even before the fix, so a run in
     * which the suite contains no NON-attribute seat would make
     * {@link #no_feature_call_is_registered_twice} pass vacuously. This asserts
     * the suite really does exercise the option/meta path.
     */
    @Test void attribute_seats_are_the_control() throws IOException {
        RWorkspace ws = buildConformanceWorkspace();

        int attributeSeats = 0;
        int nonAttributeSeats = 0;
        for (RFeatureCall fc : featureCalls(ws)) {
            if (fc.resolvedFeatureNode().isEmpty()) continue;
            if (fc.resolvedFeature().isPresent()) {
                attributeSeats++;
            } else {
                nonAttributeSeats++;
            }
        }

        assertTrue(attributeSeats > 0, "expected attribute-resolving feature calls in the suite");
        assertTrue(nonAttributeSeats > 0,
                "expected NON-attribute (choice-option / meta) feature calls in the suite — "
                        + "without them the duplicate-registration test is vacuous");
    }

    // ---------------------------------------------------------------- helpers

    private static RWorkspace buildConformanceWorkspace() throws IOException {
        List<RModel> models = new ArrayList<>();
        // CorpusCatalogue.builtinsRoot() resolves against CorpusWalker.repoRoot(), so this
        // does not depend on the working directory the way a `../test-corpus/...` literal
        // does — the suite already has one place that knows where the builtins live, and
        // a second spelling of the same path is a drift surface (Copilot R14, PR #566).
        models.addAll(parseAll(CorpusCatalogue.builtinsRoot()));
        List<RModel> subjects = parseAll(Paths.get("src/test/resources/resolution-conformance"));
        assertFalse(subjects.isEmpty(), "no conformance snippets found");
        models.addAll(subjects);
        RLinkingResult result = RWorkspace.build(models);
        return result.workspace();
    }

    private static List<RModel> parseAll(Path dir) throws IOException {
        assertTrue(Files.isDirectory(dir), "expected a directory at " + dir.toAbsolutePath());
        List<RModel> models = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList()) {
                models.add(AstBuilder.buildFromString(Files.readString(file), file.toString()));
            }
        }
        return models;
    }

    private static List<RFeatureCall> featureCalls(RWorkspace ws) {
        List<RFeatureCall> out = new ArrayList<>();
        Map<RNode, Boolean> seen = new IdentityHashMap<>();
        for (RModel model : ws.files()) {
            walk(model, out, seen);
        }
        return out;
    }

    private static void walk(RNode node, List<RFeatureCall> out, Map<RNode, Boolean> seen) {
        if (node == null || seen.put(node, Boolean.TRUE) != null) {
            return;
        }
        if (node instanceof RFeatureCall fc) {
            out.add(fc);
        }
        for (RNode child : node.children()) {
            walk(child, out, seen);
        }
    }

    /** A minimal distinct node; the index keys by identity, so nothing else matters. */
    private static final class StubNode extends RNode {
    }
}
