package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeDirectedResolverTest {

    private final BuiltinTypeRegistry builtins = BuiltinTypeRegistry.createDefault();
    private final SubtypeRelation sub = new SubtypeRelation();
    private final TypeJoin join = new TypeJoin(sub);
    private final TypeAliasSolver alias = new TypeAliasSolver();
    private final ExpressionTypeComputer typeComputer =
        new ExpressionTypeComputer(builtins, sub, join, alias);
    private final TypeInferenceEngine engine = new TypeInferenceEngine(typeComputer);
    private final TypeDirectedResolver resolver = new TypeDirectedResolver(builtins);

    // === Feature call resolution =============================================

    @Test void resolves_feature_on_data_type() {
        // Setup: Trade has attribute 'price' of type number
        var trade = new RDataType();
        trade.setName("Trade");
        var priceTc = new RTypeCall(); priceTc.setTypeName("number");
        var priceAttr = new RAttribute();
        priceAttr.setName("price"); priceAttr.setTypeCall(priceTc);
        trade.attributes().add(priceAttr);

        // RFeatureCall: receiver typed as Trade, feature = "price"
        var fc = new RFeatureCall();
        fc.setFeatureName("price");

        // Simulate: receiver's inferred type is RDataTypeRef(Trade)
        RMetaAnnotatedType receiverType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(trade));
        resolver.resolveFeatureCall(fc, receiverType);

        assertTrue(fc.resolvedFeature().isPresent());
        assertEquals("price", fc.resolvedFeature().get().name());
    }

    /**
     * THE TWO SLOTS MUST NEVER DISAGREE. {@code resolvedFeatureNode} is authoritative and
     * holds any feature kind; the legacy {@code resolvedFeature} is typed to
     * {@link RAttribute} and so can only hold one. If a call is re-resolved and the new
     * binding is NOT an attribute, leaving the old attribute in the legacy slot would
     * leave the two disagreeing — and 80-odd call sites still read the legacy one
     * (Copilot R19, PR #566).
     *
     * <p>Unreachable through the engine today: the Category-1 arm is gated on
     * {@code resolvedFeatureNode().isEmpty()}, so a bound call is never re-resolved. This
     * locks the resolver's own contract, since the method is public and does not get to
     * assume that gate.
     */
    @Test void rebinding_to_a_non_attribute_clears_the_legacy_slot() {
        var cashTypeCall = new RTypeCall();
        cashTypeCall.setTypeName("Cash");
        var option = new com.regnosys.rosetta.ast.supporting.RChoiceOption();
        option.setTypeCall(cashTypeCall);
        var choice = new com.regnosys.rosetta.ast.types.RChoice();
        choice.setName("Payout");
        choice.options().add(option);
        var choiceRef = new RChoiceTypeRef("Payout", List.of(), choice);

        var fc = new RFeatureCall();
        fc.setFeatureName("Cash");

        // An earlier attribute binding sitting in the legacy slot.
        var stale = new RAttribute();
        stale.setName("Cash");
        fc.setResolvedFeature(stale);
        assertTrue(fc.resolvedFeature().isPresent(), "precondition: the legacy slot is populated");

        resolver.resolveFeatureCall(fc, RMetaAnnotatedType.withNoMeta(choiceRef));

        assertTrue(fc.resolvedFeatureNode().isPresent(), "the option must bind");
        assertInstanceOf(com.regnosys.rosetta.ast.supporting.RChoiceOption.class,
                fc.resolvedFeatureNode().get());
        assertTrue(fc.resolvedFeature().isEmpty(),
                "the legacy attribute slot must be cleared when the new binding is not an "
                        + "attribute, or the two slots disagree");
    }

    @Test void feature_not_found_leaves_unresolved() {
        var trade = new RDataType();
        trade.setName("Trade");
        // No attributes on Trade

        var fc = new RFeatureCall();
        fc.setFeatureName("nonexistent");

        RMetaAnnotatedType receiverType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(trade));
        resolver.resolveFeatureCall(fc, receiverType);

        assertTrue(fc.resolvedFeature().isEmpty());
    }

    @Test void feature_on_inherited_type() {
        // Parent has attribute 'id'
        var testResolver = new TestSymbolResolver();
        var parent = new RDataType(); parent.setName("Parent");
        var idTc = new RTypeCall(); idTc.setTypeName("string");
        var idAttr = new RAttribute();
        idAttr.setName("id"); idAttr.setTypeCall(idTc);
        parent.attributes().add(idAttr);

        var child = new RDataType(); child.setName("Child");
        testResolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);

        var fc = new RFeatureCall();
        fc.setFeatureName("id");

        RMetaAnnotatedType receiverType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(child));
        resolver.resolveFeatureCall(fc, receiverType);

        assertTrue(fc.resolvedFeature().isPresent());
        assertEquals("id", fc.resolvedFeature().get().name());
        Reference.reachabilityFence(testResolver);
    }

    // === Deep feature call resolution ========================================

    @Test void deep_feature_searches_transitively() {
        // Trade -> Party { name: string }
        var deepResolver = new TestSymbolResolver();
        var party = new RDataType(); party.setName("Party");
        var nameTc = new RTypeCall(); nameTc.setTypeName("string");
        // "string" is a builtin — no referencedTypeId set; TypeDirectedResolver falls back by name
        var nameAttr = new RAttribute();
        nameAttr.setName("name"); nameAttr.setTypeCall(nameTc);
        party.attributes().add(nameAttr);
        party.attachToWorkspace(deepResolver);
        deepResolver.bind("test", "Party", party);

        var partyTc = new RTypeCall(); partyTc.setTypeName("Party");
        partyTc.attachToWorkspace(deepResolver);
        partyTc.setReferencedTypeId(deepResolver.idFor("test", "Party"));
        var partyAttr = new RAttribute();
        partyAttr.setName("party"); partyAttr.setTypeCall(partyTc);

        var trade = new RDataType(); trade.setName("Trade");
        trade.attributes().add(partyAttr);

        var dfc = new RDeepFeatureCall();
        dfc.setFeatureName("name");

        RMetaAnnotatedType receiverType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(trade));
        resolver.resolveDeepFeatureCall(dfc, receiverType);

        assertTrue(dfc.resolvedFeature().isPresent());
        assertEquals("name", dfc.resolvedFeature().get().name());
        Reference.reachabilityFence(deepResolver);
    }

    // === Record type features ================================================

    @Test void date_features_resolved() {
        // date has features: day, month, year
        var fc = new RFeatureCall();
        fc.setFeatureName("year");

        RMetaAnnotatedType receiverType = RMetaAnnotatedType.withNoMeta(RRecordType.DATE);
        resolver.resolveFeatureCall(fc, receiverType);

        // Record type features don't resolve to RAttribute (they're structural).
        // Feature call on record type is still valid but resolvedFeature stays empty
        // because RecordFeature is not an RAttribute. Type inference handles this
        // via the RecordType's feature list. This is correct per spec.
        assertTrue(fc.resolvedFeature().isEmpty());
    }

    // === Missing receiver type ===============================================

    @Test void missing_receiver_type_no_resolution() {
        var fc = new RFeatureCall();
        fc.setFeatureName("anything");
        resolver.resolveFeatureCall(fc, RMetaAnnotatedType.MISSING);
        assertTrue(fc.resolvedFeature().isEmpty());
    }
}
