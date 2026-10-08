package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.index.ReferenceIndex;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression for the {@link TypeInferenceEngine} listener-wiring contract,
 * accumulated across Copilot PR #76 R11 + R12 + R14:
 *
 * <ul>
 *   <li><b>R11 F1</b> — when both {@link TypeInferenceEngine#setResolver} and
 *       {@link TypeInferenceEngine#setReferenceIndex} have received non-null
 *       values, the resolver's {@code ResolutionListener} is wired to
 *       {@code referenceIndex::registerReference} so every resolution-stamp
 *       setter inside {@link TypeDirectedResolver} pairs with a registration
 *       on the reverse index (mirroring the pass-4/5 contract).</li>
 *   <li><b>R12 F1</b> — configuration order is irrelevant; setting
 *       {@code referenceIndex} before {@code resolver} also wires the
 *       listener.</li>
 *   <li><b>R14 F1</b> — setting {@code referenceIndex} to {@code null} after a
 *       previous non-null set must clear the resolver's listener so the
 *       optional-dependency javadoc ("if null ... registration is skipped")
 *       holds beyond initial configuration.</li>
 * </ul>
 *
 * <p>The tests verify the contract behaviourally: fire one of the resolver's
 * {@code resolveX} methods (which calls {@code notifyResolved} via the
 * listener on success) and inspect the {@link ReferenceIndex} reverse-lookup.
 * Avoids reflection on the resolver's private {@code resolutionListener} field
 * and exposes no test-only accessor on production classes.
 */
class TypeInferenceEngineListenerWiringTest {

    private final BuiltinTypeRegistry builtins = BuiltinTypeRegistry.createDefault();
    private final SubtypeRelation sub = new SubtypeRelation();
    private final TypeJoin join = new TypeJoin(sub);
    private final TypeAliasSolver alias = new TypeAliasSolver();
    private final ExpressionTypeComputer typeComputer =
        new ExpressionTypeComputer(builtins, sub, join, alias);

    @Test void resolver_first_then_index_wires_listener() {
        var engine = new TypeInferenceEngine(typeComputer);
        var resolver = new TypeDirectedResolver(builtins);
        var index = new ReferenceIndex();
        engine.setResolver(resolver);
        engine.setReferenceIndex(index);

        var fixture = new ResolveFixture();
        resolver.resolveFeatureCall(fixture.fc, fixture.receiverType);

        assertTrue(index.findReferences(fixture.priceAttr).contains(fixture.fc),
                "R11 F1: feature call resolution must register against the reference index");
    }

    @Test void index_first_then_resolver_wires_listener() {
        var engine = new TypeInferenceEngine(typeComputer);
        var resolver = new TypeDirectedResolver(builtins);
        var index = new ReferenceIndex();
        // R12 F1 — order-independent.
        engine.setReferenceIndex(index);
        engine.setResolver(resolver);

        var fixture = new ResolveFixture();
        resolver.resolveFeatureCall(fixture.fc, fixture.receiverType);

        assertTrue(index.findReferences(fixture.priceAttr).contains(fixture.fc),
                "R12 F1: listener must be wired regardless of which setter is called first");
    }

    @Test void clearing_index_after_wired_clears_listener() {
        // R14 F1 — javadoc on setReferenceIndex promises "if null ... registration
        // is skipped". Without explicit clearing, a previously-wired closure leaked
        // beyond a null re-set and resolution-stamp setters kept registering against
        // the old index.
        var engine = new TypeInferenceEngine(typeComputer);
        var resolver = new TypeDirectedResolver(builtins);
        var index = new ReferenceIndex();
        engine.setResolver(resolver);
        engine.setReferenceIndex(index);

        // Precondition — listener wired.
        var pre = new ResolveFixture();
        resolver.resolveFeatureCall(pre.fc, pre.receiverType);
        assertTrue(index.findReferences(pre.priceAttr).contains(pre.fc),
                "precondition: listener wired before null clear");

        // Clear the index — listener must be detached.
        engine.setReferenceIndex(null);

        var post = new ResolveFixture();
        resolver.resolveFeatureCall(post.fc, post.receiverType);

        // The new resolution must NOT register against the old index.
        assertFalse(index.findReferences(post.priceAttr).contains(post.fc),
                "R14 F1: setReferenceIndex(null) must clear the previously-wired listener");
    }

    @Test void index_only_no_resolver_does_not_npe() {
        var engine = new TypeInferenceEngine(typeComputer);
        // No resolver wired — the rewiring helper must short-circuit on null
        // resolver instead of NPE'ing.
        assertDoesNotThrow(() -> engine.setReferenceIndex(new ReferenceIndex()));
        assertDoesNotThrow(() -> engine.setReferenceIndex(null));
    }

    @Test void clearing_resolver_after_wired_detaches_old_listener() {
        // R16 F1 — earlier R14 fix relied on the rewireResolutionListener helper
        // alone, which short-circuits on a null new resolver and so left the OLD
        // resolver's listener intact (still pointing at index::registerReference).
        // Callers that still held a direct reference to the orphaned resolver
        // could keep registering into the index post-unset. The R16 fix detaches
        // the previous resolver's listener before overwriting the field.
        var engine = new TypeInferenceEngine(typeComputer);
        var resolver = new TypeDirectedResolver(builtins);
        var index = new ReferenceIndex();
        engine.setResolver(resolver);
        engine.setReferenceIndex(index);

        // Precondition — listener wired on the resolver, registers into the index.
        var pre = new ResolveFixture();
        resolver.resolveFeatureCall(pre.fc, pre.receiverType);
        assertTrue(index.findReferences(pre.priceAttr).contains(pre.fc),
                "precondition: listener wired before resolver clear");

        // Unset the resolver — old resolver's listener must be detached.
        assertDoesNotThrow(() -> engine.setResolver(null));

        // Fire a resolution via the now-orphaned old resolver. If the listener
        // was correctly detached, this must NOT register into the index.
        var post = new ResolveFixture();
        resolver.resolveFeatureCall(post.fc, post.receiverType);
        assertFalse(index.findReferences(post.priceAttr).contains(post.fc),
                "R16 F1: setResolver(null) must detach the old resolver's listener");
    }

    @Test void swapping_resolver_detaches_old_listener_wires_new() {
        // R16 F1 — same defect class as the unset case: if the engine swaps
        // its resolver from R1 to R2, R1 must be detached so any caller that
        // still holds R1 can't keep registering into the engine's index.
        var engine = new TypeInferenceEngine(typeComputer);
        var index = new ReferenceIndex();
        var r1 = new TypeDirectedResolver(builtins);
        engine.setResolver(r1);
        engine.setReferenceIndex(index);

        // Swap to a fresh resolver instance.
        var r2 = new TypeDirectedResolver(builtins);
        engine.setResolver(r2);

        // r1 is orphaned — must NOT register into the engine's index.
        var f1 = new ResolveFixture();
        r1.resolveFeatureCall(f1.fc, f1.receiverType);
        assertFalse(index.findReferences(f1.priceAttr).contains(f1.fc),
                "R16 F1: swap replaces old resolver — old listener must be detached");

        // r2 is the new live resolver — must register into the engine's index.
        var f2 = new ResolveFixture();
        r2.resolveFeatureCall(f2.fc, f2.receiverType);
        assertTrue(index.findReferences(f2.priceAttr).contains(f2.fc),
                "after swap, new resolver's listener is wired to the engine's index");
    }

    @Test void re_setting_index_to_new_instance_rewires_listener() {
        var engine = new TypeInferenceEngine(typeComputer);
        var resolver = new TypeDirectedResolver(builtins);
        engine.setResolver(resolver);

        var idx1 = new ReferenceIndex();
        engine.setReferenceIndex(idx1);
        var fixture1 = new ResolveFixture();
        resolver.resolveFeatureCall(fixture1.fc, fixture1.receiverType);
        assertTrue(idx1.findReferences(fixture1.priceAttr).contains(fixture1.fc),
                "first index receives the first registration");

        var idx2 = new ReferenceIndex();
        engine.setReferenceIndex(idx2);
        var fixture2 = new ResolveFixture();
        resolver.resolveFeatureCall(fixture2.fc, fixture2.receiverType);

        // The new resolution must register against the new index, not the old one.
        assertTrue(idx2.findReferences(fixture2.priceAttr).contains(fixture2.fc),
                "after re-set, listener registers against the new index");
        assertFalse(idx1.findReferences(fixture2.priceAttr).contains(fixture2.fc),
                "after re-set, listener no longer registers against the old index");
    }

    /**
     * Lightweight RDataType + RAttribute + RFeatureCall fixture sharing the same
     * shape as {@code TypeDirectedResolverTest#resolves_feature_on_data_type}.
     * Independent instances per test method ensure no cross-test pollution.
     */
    private static final class ResolveFixture {
        final RDataType trade;
        final RAttribute priceAttr;
        final RFeatureCall fc;
        final RMetaAnnotatedType receiverType;

        ResolveFixture() {
            trade = new RDataType();
            trade.setName("Trade");
            var priceTc = new RTypeCall();
            priceTc.setTypeName("number");
            priceAttr = new RAttribute();
            priceAttr.setName("price");
            priceAttr.setTypeCall(priceTc);
            trade.attributes().add(priceAttr);

            fc = new RFeatureCall();
            fc.setFeatureName("price");
            receiverType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(trade));
        }
    }
}
