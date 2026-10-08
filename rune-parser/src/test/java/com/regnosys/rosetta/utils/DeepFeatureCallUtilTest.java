package com.regnosys.rosetta.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil.AttributeTypeResolver;

/**
 * Phase X T2 — DeepFeatureCallUtil unit tests.
 *
 * <p>Covers the 8 acceptance cases per controller T2 brief + plan
 * the development plan "2026-05-19-phase-x-port-3-generators" § T2 step 4:
 * shallow / 2-hop / 3+ hop / {@link RChoiceTypeRef} unwrap / empty graph /
 * multiple paths / not-reachable / optional-cardinality.
 *
 * <p>Fixtures are built without a {@code SymbolResolver} workspace. The
 * {@link AttributeTypeResolver} functional interface is injected directly,
 * so tests can wire attribute-to-RType lookups in a stand-alone way.
 */
class DeepFeatureCallUtilTest {

    // === Fixture helpers =====================================================

    /**
     * Mutable test fixture that records the {@link RType} of every
     * {@link RAttribute} it builds. Acts as the {@link AttributeTypeResolver}
     * passed to {@link DeepFeatureCallUtil} so unit tests can assert paths
     * without a full workspace setup.
     */
    private static final class Fixture {
        final Map<RAttribute, RType> attrTypes = new IdentityHashMap<>();

        RDataType dataType(String name, List<RAttribute> attrs) {
            RDataType dt = new RDataType();
            dt.setName(name);
            dt.attributes().addAll(attrs);
            return dt;
        }

        RChoice choice(String name) {
            RChoice ch = new RChoice();
            ch.setName(name);
            return ch;
        }

        /** Build an attribute typed by an arbitrary {@link RType}. */
        RAttribute attribute(String name, RType type) {
            RAttribute attr = new RAttribute();
            attr.setName(name);
            RTypeCall tc = new RTypeCall();
            tc.setTypeName(type.name());
            attr.setTypeCall(tc);
            attrTypes.put(attr, type);
            return attr;
        }

        /**
         * Build an attribute typed by an arbitrary {@link RType} with explicit
         * (inf, sup) cardinality. inf=0/sup=1 models the single-optional case
         * used by the {@code findDeepFeatureMap} eligibility check.
         */
        RAttribute attribute(String name, RType type, int inf, int sup) {
            RAttribute attr = attribute(name, type);
            RCardinality card = new RCardinality();
            card.setInf(inf);
            card.setSup(sup);
            attr.setCardinality(card);
            return attr;
        }

        DeepFeatureCallUtil util() {
            return new DeepFeatureCallUtil(attrTypes::get);
        }
    }

    // === Tests ===============================================================

    @Test
    void findsShallowPath_singleHop() {
        // type Foo { x int }
        Fixture f = new Fixture();
        RAttribute xAttr = f.attribute("x", RBasicType.ANY);
        RDataType foo = f.dataType("Foo", List.of(xAttr));

        List<List<RAttribute>> paths = f.util()
                .findDeepFeaturePaths(new RDataTypeRef(foo), xAttr);

        assertEquals(1, paths.size(), "expected exactly 1 path to a direct attribute");
        assertEquals(1, paths.get(0).size(), "shallow path should be 1 hop");
        assertEquals(xAttr, paths.get(0).get(0));
    }

    @Test
    void findsTwoHopPath() {
        // type Inner { x int }
        // type Outer { inner Inner }
        Fixture f = new Fixture();
        RAttribute xAttr = f.attribute("x", RBasicType.ANY);
        RDataType inner = f.dataType("Inner", List.of(xAttr));
        RAttribute innerAttr = f.attribute("inner", new RDataTypeRef(inner));
        RDataType outer = f.dataType("Outer", List.of(innerAttr));

        // Target is an artificial sibling-RAttribute with the SAME RType + cardinality
        // as Inner.x — DeepFeatureCallUtil.match compares attrs by type/multi-ness, not identity.
        RAttribute target = f.attribute("x", RBasicType.ANY);

        List<List<RAttribute>> paths = f.util()
                .findDeepFeaturePaths(new RDataTypeRef(outer), target);

        assertEquals(1, paths.size(), "expected exactly 1 path");
        assertEquals(2, paths.get(0).size(), "path should traverse Outer.inner → Inner.x");
        assertEquals(innerAttr, paths.get(0).get(0));
        assertEquals(xAttr, paths.get(0).get(1));
    }

    @Test
    void findsThreeHopPath() {
        // type L3 { leaf int }
        // type L2 { l3 L3 }
        // type L1 { l2 L2 }
        Fixture f = new Fixture();
        RAttribute leafAttr = f.attribute("leaf", RBasicType.ANY);
        RDataType l3 = f.dataType("L3", List.of(leafAttr));
        RAttribute l3Attr = f.attribute("l3", new RDataTypeRef(l3));
        RDataType l2 = f.dataType("L2", List.of(l3Attr));
        RAttribute l2Attr = f.attribute("l2", new RDataTypeRef(l2));
        RDataType l1 = f.dataType("L1", List.of(l2Attr));

        RAttribute target = f.attribute("leaf", RBasicType.ANY);

        List<List<RAttribute>> paths = f.util()
                .findDeepFeaturePaths(new RDataTypeRef(l1), target);

        assertEquals(1, paths.size());
        assertEquals(3, paths.get(0).size(), "path should traverse 3 hops");
        assertEquals(Arrays.asList(l2Attr, l3Attr, leafAttr), paths.get(0));
    }

    @Test
    void unwrapsRChoiceTypeRef_onIntermediateType() {
        // choice Pick: { picked int }   (test fixture; the real choice's
        //                                RDataType bridge is constructed
        //                                via a test-side override of
        //                                DeepFeatureCallUtil.asRDataType)
        // type Outer { picker Pick }
        Fixture f = new Fixture();
        RAttribute pickedAttr = f.attribute("picked", RBasicType.ANY);
        final RDataType pickedHolder = f.dataType("PickedHolder", List.of(pickedAttr));
        RChoice pick = f.choice("Pick");
        RChoiceTypeRef pickRef = new RChoiceTypeRef("Pick", List.of(), pick);
        RAttribute pickerAttr = f.attribute("picker", pickRef);
        RDataType outer = f.dataType("Outer", List.of(pickerAttr));

        // RChoiceTypeRef.asRDataType() is wired to use the choice's astNode in
        // production code (T2 step 6). For the unit test, override the bridge
        // hook on DeepFeatureCallUtil itself so the test is isolated from the
        // asRDataType() implementation (which lives on the final
        // RChoiceTypeRef class).
        DeepFeatureCallUtil util = new DeepFeatureCallUtil(f.attrTypes::get) {
            @Override
            protected RDataType asRDataType(RChoiceTypeRef ref) {
                return pickedHolder;
            }
        };

        RAttribute target = f.attribute("picked", RBasicType.ANY);

        List<List<RAttribute>> paths = util
                .findDeepFeaturePaths(new RDataTypeRef(outer), target);

        assertEquals(1, paths.size(), "choice should unwrap and expose nested attribute");
        assertEquals(2, paths.get(0).size());
        assertEquals(pickerAttr, paths.get(0).get(0));
        assertEquals(pickedAttr, paths.get(0).get(1));
    }

    @Test
    void emptyGraph_returnsEmptyList() {
        // type Foo {} — no attributes at all.
        Fixture f = new Fixture();
        RDataType foo = f.dataType("Foo", List.of());

        // Target attribute that simply doesn't exist anywhere reachable.
        RAttribute target = f.attribute("missing", RBasicType.ANY);

        List<List<RAttribute>> paths = f.util()
                .findDeepFeaturePaths(new RDataTypeRef(foo), target);

        assertNotNull(paths);
        assertTrue(paths.isEmpty(), "no paths should be found in an empty graph");
    }

    @Test
    void multiplePaths_toSameAttributeShape() {
        // type Foo { a Leaf, b Leaf }
        // type Leaf { value int }
        // Target = artificial leaf-value sibling: both a.value and b.value
        // match by shape, so both paths must be returned.
        Fixture f = new Fixture();
        RAttribute aValue = f.attribute("value", RBasicType.ANY);
        RDataType leafA = f.dataType("Leaf", List.of(aValue));
        RAttribute bValue = f.attribute("value", RBasicType.ANY);
        RDataType leafB = f.dataType("Leaf", List.of(bValue));
        RAttribute aAttr = f.attribute("a", new RDataTypeRef(leafA));
        RAttribute bAttr = f.attribute("b", new RDataTypeRef(leafB));
        RDataType foo = f.dataType("Foo", List.of(aAttr, bAttr));

        RAttribute target = f.attribute("value", RBasicType.ANY);

        List<List<RAttribute>> paths = f.util()
                .findDeepFeaturePaths(new RDataTypeRef(foo), target);

        assertEquals(2, paths.size(), "both Foo.a.value and Foo.b.value should match");
        // Order: depth-first, matching upstream iteration over getAllAttributes.
        List<List<RAttribute>> expected = new ArrayList<>();
        expected.add(List.of(aAttr, aValue));
        expected.add(List.of(bAttr, bValue));
        assertEquals(expected, paths);
    }

    @Test
    void targetNotReachable_returnsEmptyList() {
        // type Foo { x int, y boolean } — target type is RMissingType, never
        // matches any attribute on Foo, and Foo has no recursive sub-types.
        Fixture f = new Fixture();
        RAttribute xAttr = f.attribute("x", RBasicType.ANY);
        RAttribute yAttr = f.attribute("y", RBasicType.BOOLEAN);
        RDataType foo = f.dataType("Foo", List.of(xAttr, yAttr));

        // Target has an RType (RMissingType.INSTANCE) that no Foo attr has —
        // and there are no nested data types to recurse into.
        RAttribute target = f.attribute("doesnotexist", RMissingType.INSTANCE);

        List<List<RAttribute>> paths = f.util()
                .findDeepFeaturePaths(new RDataTypeRef(foo), target);

        assertTrue(paths.isEmpty(), "unreachable attribute should yield no paths");
    }

    @Test
    void optionalCardinality_attribute_returnsSingleHopPath() {
        // type Foo { x int (0..1) } — single-optional attribute.
        // The path-finder should treat it identically to (1..1)/etc — the
        // cardinality only matters for findDeepFeatureMap eligibility, NOT
        // for the path traversal itself. This test pins that contract.
        Fixture f = new Fixture();
        RAttribute xAttr = f.attribute("x", RBasicType.ANY, 0, 1);
        RDataType foo = f.dataType("Foo", List.of(xAttr));

        List<List<RAttribute>> paths = f.util()
                .findDeepFeaturePaths(new RDataTypeRef(foo), xAttr);

        assertEquals(1, paths.size());
        assertEquals(1, paths.get(0).size());
        assertEquals(xAttr, paths.get(0).get(0));

        // ALSO assert isMulti() honours the cardinality: (0..1) is NOT multi.
        DeepFeatureCallUtil util = f.util();
        assertEquals(false, util.isMulti(xAttr),
                "(0..1) attribute should not register as multi");

        // Compare against an unbounded counterpart.
        RAttribute multiAttr = f.attribute("xs", RBasicType.ANY);
        RCardinality multiCard = new RCardinality();
        multiCard.setInf(0);
        multiCard.setSup(BigInteger.valueOf(-1)); // UNBOUNDED sentinel
        multiCard.setUnbounded(true);
        multiAttr.setCardinality(multiCard);
        assertEquals(true, util.isMulti(multiAttr),
                "unbounded attribute should register as multi");
    }
}
