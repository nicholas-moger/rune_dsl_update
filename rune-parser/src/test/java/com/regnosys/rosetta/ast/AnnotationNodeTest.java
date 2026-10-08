package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.annotations.RAnnotation;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathExpression;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment;
import com.regnosys.rosetta.ast.annotations.RAnnotationQualifier;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.annotations.RLabelAnnotation;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for annotation-related AST nodes (Task 5).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, list fields, boolean getters, and the
 * {@code children()} traversal.
 */
class AnnotationNodeTest extends BaseAstTest {

    // =========================================================================
    // RAnnotation
    // =========================================================================

    @Test
    void rAnnotation_defaultState() {
        RAnnotation ann = new RAnnotation();
        assertNull(ann.name());
        assertEquals(Optional.empty(), ann.prefix());
        assertEquals(Optional.empty(), ann.definition());
        assertTrue(ann.attributes().isEmpty());
        assertTrue(ann.children().isEmpty());
    }

    @Test
    void rAnnotation_setName() {
        RAnnotation ann = new RAnnotation();
        ann.setName("metadata");

        assertEquals("metadata", ann.name());
    }

    @Test
    void rAnnotation_setPrefix() {
        RAnnotation ann = new RAnnotation();
        ann.setName("metadata");
        ann.setPrefix("myPrefix");

        assertEquals(Optional.of("myPrefix"), ann.prefix());
    }

    @Test
    void rAnnotation_setDefinition() {
        RAnnotation ann = new RAnnotation();
        ann.setName("metadata");
        ann.setDefinition("Describes metadata annotations.");

        assertEquals(Optional.of("Describes metadata annotations."), ann.definition());
    }

    @Test
    void rAnnotation_implementsRDefinable() {
        RAnnotation ann = new RAnnotation();
        assertInstanceOf(RDefinable.class, ann);
    }

    @Test
    void rAnnotation_extendsRRootElement() {
        RAnnotation ann = new RAnnotation();
        assertInstanceOf(RRootElement.class, ann);
        assertInstanceOf(RNode.class, ann);
    }

    @Test
    void rAnnotation_addAttributes() {
        RAnnotation ann = new RAnnotation();
        ann.setName("metadata");

        RAttribute attr1 = new RAttribute();
        attr1.setName("key");
        RTypeCall tc1 = new RTypeCall();
        tc1.setTypeName("string");
        attr1.setTypeCall(tc1);

        RAttribute attr2 = new RAttribute();
        attr2.setName("scheme");
        RTypeCall tc2 = new RTypeCall();
        tc2.setTypeName("string");
        attr2.setTypeCall(tc2);

        ann.attributes().add(attr1);
        ann.attributes().add(attr2);

        assertEquals(2, ann.attributes().size());
        assertEquals("key", ann.attributes().get(0).name());
        assertEquals("scheme", ann.attributes().get(1).name());
    }

    @Test
    void rAnnotation_childrenIncludesAttributes() {
        RAnnotation ann = new RAnnotation();
        ann.setName("metadata");

        RAttribute attr = new RAttribute();
        attr.setName("key");
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("string");
        attr.setTypeCall(tc);
        ann.attributes().add(attr);

        List<? extends RNode> children = ann.children();
        assertEquals(1, children.size());
        assertSame(attr, children.get(0));
    }

    @Test
    void rAnnotation_noPrefix() {
        RAnnotation ann = new RAnnotation();
        ann.setName("rootType");
        assertEquals(Optional.empty(), ann.prefix());
    }

    // =========================================================================
    // RAnnotationRef
    // =========================================================================

    @Test
    void rAnnotationRef_defaultState() {
        RAnnotationRef ref = new RAnnotationRef();
        assertNull(ref.annotationName());
        assertEquals(Optional.empty(), ref.qualifierName());
        assertTrue(ref.qualifiers().isEmpty());
        assertTrue(ref.children().isEmpty());
    }

    @Test
    void rAnnotationRef_setAnnotationName() {
        RAnnotationRef ref = new RAnnotationRef();
        ref.setAnnotationName("metadata");

        assertEquals("metadata", ref.annotationName());
    }

    @Test
    void rAnnotationRef_setQualifierName() {
        RAnnotationRef ref = new RAnnotationRef();
        ref.setAnnotationName("metadata");
        ref.setQualifierName("key");

        assertEquals(Optional.of("key"), ref.qualifierName());
    }

    @Test
    void rAnnotationRef_noQualifierName() {
        RAnnotationRef ref = new RAnnotationRef();
        ref.setAnnotationName("rootType");
        assertEquals(Optional.empty(), ref.qualifierName());
    }

    @Test
    void rAnnotationRef_addQualifiers() {
        RAnnotationRef ref = new RAnnotationRef();
        ref.setAnnotationName("docReference");

        RAnnotationQualifier q1 = new RAnnotationQualifier();
        q1.setKey("rationale");
        q1.setValue("Required for reporting");
        q1.setAttributeRef(false);

        RAnnotationQualifier q2 = new RAnnotationQualifier();
        q2.setKey("path");
        q2.setValue("Trade -> tradeLeg");
        q2.setAttributeRef(true);

        ref.qualifiers().add(q1);
        ref.qualifiers().add(q2);

        assertEquals(2, ref.qualifiers().size());
        assertEquals("rationale", ref.qualifiers().get(0).key());
        assertEquals("path", ref.qualifiers().get(1).key());
    }

    @Test
    void rAnnotationRef_childrenIncludesQualifiers() {
        RAnnotationRef ref = new RAnnotationRef();
        ref.setAnnotationName("docReference");

        RAnnotationQualifier q = new RAnnotationQualifier();
        q.setKey("rationale");
        q.setValue("Required");
        q.setAttributeRef(false);
        ref.qualifiers().add(q);

        List<? extends RNode> children = ref.children();
        assertEquals(1, children.size());
        assertSame(q, children.get(0));
    }

    @Test
    void rAnnotationRef_extendsRNode() {
        RAnnotationRef ref = new RAnnotationRef();
        assertInstanceOf(RNode.class, ref);
        assertFalse(RRootElement.class.isAssignableFrom(RAnnotationRef.class));
    }

    // =========================================================================
    // RAnnotationQualifier
    // =========================================================================

    @Test
    void rAnnotationQualifier_defaultState() {
        RAnnotationQualifier q = new RAnnotationQualifier();
        assertNull(q.key());
        assertNull(q.value());
        assertFalse(q.isAttributeRef());
    }

    @Test
    void rAnnotationQualifier_setKeyAndStringValue() {
        RAnnotationQualifier q = new RAnnotationQualifier();
        q.setKey("rationale");
        q.setValue("Required for compliance");
        q.setAttributeRef(false);

        assertEquals("rationale", q.key());
        assertEquals("Required for compliance", q.value());
        assertFalse(q.isAttributeRef());
    }

    @Test
    void rAnnotationQualifier_setAttributeRefValue() {
        RAnnotationQualifier q = new RAnnotationQualifier();
        q.setKey("path");
        q.setValue("Trade -> tradeLeg -> price");
        q.setAttributeRef(true);

        assertEquals("path", q.key());
        assertEquals("Trade -> tradeLeg -> price", q.value());
        assertTrue(q.isAttributeRef());
    }

    @Test
    void rAnnotationQualifier_isLeafNode() {
        RAnnotationQualifier q = new RAnnotationQualifier();
        q.setKey("key");
        q.setValue("val");
        assertTrue(q.children().isEmpty());
    }

    @Test
    void rAnnotationQualifier_extendsRNode() {
        RAnnotationQualifier q = new RAnnotationQualifier();
        assertInstanceOf(RNode.class, q);
        assertFalse(RRootElement.class.isAssignableFrom(RAnnotationQualifier.class));
    }

    // =========================================================================
    // RLabelAnnotation
    // =========================================================================

    @Test
    void rLabelAnnotation_defaultState() {
        RLabelAnnotation la = new RLabelAnnotation();
        assertEquals(Optional.empty(), la.forPath());
        assertEquals(Optional.empty(), la.asPath());
        assertNull(la.label());
        assertTrue(la.children().isEmpty());
    }

    @Test
    void rLabelAnnotation_setLabel() {
        RLabelAnnotation la = new RLabelAnnotation();
        la.setLabel("Party Role");

        assertEquals("Party Role", la.label());
    }

    @Test
    void rLabelAnnotation_setForPath() {
        RLabelAnnotation la = new RLabelAnnotation();
        la.setLabel("Party Role");

        RAnnotationPathExpression forPath = new RAnnotationPathExpression();
        forPath.setRoot("trade");
        forPath.setRootIsItem(false);
        la.setForPath(forPath);

        assertTrue(la.forPath().isPresent());
        assertEquals("trade", la.forPath().get().root());
    }

    @Test
    void rLabelAnnotation_setAsPath() {
        RLabelAnnotation la = new RLabelAnnotation();
        la.setLabel("My Label");

        RAnnotationPathExpression asPath = new RAnnotationPathExpression();
        asPath.setRoot("partyRole");
        asPath.setRootIsItem(false);
        la.setAsPath(asPath);

        assertTrue(la.asPath().isPresent());
        assertEquals("partyRole", la.asPath().get().root());
    }

    @Test
    void rLabelAnnotation_childrenIncludesPaths() {
        RLabelAnnotation la = new RLabelAnnotation();
        la.setLabel("My Label");

        RAnnotationPathExpression forPath = new RAnnotationPathExpression();
        forPath.setRoot("trade");
        forPath.setRootIsItem(false);
        la.setForPath(forPath);

        RAnnotationPathExpression asPath = new RAnnotationPathExpression();
        asPath.setRoot("item");
        asPath.setRootIsItem(true);
        la.setAsPath(asPath);

        List<? extends RNode> children = la.children();
        assertEquals(2, children.size());
        assertSame(forPath, children.get(0));
        assertSame(asPath, children.get(1));
    }

    @Test
    void rLabelAnnotation_childrenWithOnlyForPath() {
        RLabelAnnotation la = new RLabelAnnotation();
        la.setLabel("My Label");

        RAnnotationPathExpression forPath = new RAnnotationPathExpression();
        forPath.setRoot("trade");
        forPath.setRootIsItem(false);
        la.setForPath(forPath);

        List<? extends RNode> children = la.children();
        assertEquals(1, children.size());
        assertSame(forPath, children.get(0));
    }

    @Test
    void rLabelAnnotation_childrenEmpty() {
        RLabelAnnotation la = new RLabelAnnotation();
        la.setLabel("Simple Label");
        assertTrue(la.children().isEmpty());
    }

    @Test
    void rLabelAnnotation_extendsRNode() {
        RLabelAnnotation la = new RLabelAnnotation();
        assertInstanceOf(RNode.class, la);
        assertFalse(RRootElement.class.isAssignableFrom(RLabelAnnotation.class));
    }

    // =========================================================================
    // RRuleReferenceAnnotation
    // =========================================================================

    @Test
    void rRuleReferenceAnnotation_defaultState() {
        RRuleReferenceAnnotation rra = new RRuleReferenceAnnotation();
        assertEquals(Optional.empty(), rra.forPath());
        assertEquals(Optional.empty(), rra.ruleName());
        assertTrue(rra.children().isEmpty());
    }

    @Test
    void rRuleReferenceAnnotation_setRuleName() {
        RRuleReferenceAnnotation rra = new RRuleReferenceAnnotation();
        rra.setRuleName("com.example.TradePartyRole");

        assertEquals(Optional.of("com.example.TradePartyRole"), rra.ruleName());
    }

    @Test
    void rRuleReferenceAnnotation_emptyRule() {
        // When EMPTY keyword is used, ruleName stays null
        RRuleReferenceAnnotation rra = new RRuleReferenceAnnotation();
        assertEquals(Optional.empty(), rra.ruleName());
    }

    @Test
    void rRuleReferenceAnnotation_setForPath() {
        RRuleReferenceAnnotation rra = new RRuleReferenceAnnotation();
        rra.setRuleName("TradePartyRole");

        RAnnotationPathExpression forPath = new RAnnotationPathExpression();
        forPath.setRoot("trade");
        forPath.setRootIsItem(false);

        RAnnotationPathSegment seg = new RAnnotationPathSegment();
        seg.setName("partyRole");
        seg.setDeep(false);
        forPath.segments().add(seg);

        rra.setForPath(forPath);

        assertTrue(rra.forPath().isPresent());
        assertEquals("trade", rra.forPath().get().root());
        assertEquals(1, rra.forPath().get().segments().size());
    }

    @Test
    void rRuleReferenceAnnotation_childrenIncludesForPath() {
        RRuleReferenceAnnotation rra = new RRuleReferenceAnnotation();
        rra.setRuleName("SomeRule");

        RAnnotationPathExpression forPath = new RAnnotationPathExpression();
        forPath.setRoot("trade");
        forPath.setRootIsItem(false);
        rra.setForPath(forPath);

        List<? extends RNode> children = rra.children();
        assertEquals(1, children.size());
        assertSame(forPath, children.get(0));
    }

    @Test
    void rRuleReferenceAnnotation_childrenEmptyWithoutForPath() {
        RRuleReferenceAnnotation rra = new RRuleReferenceAnnotation();
        rra.setRuleName("SomeRule");
        assertTrue(rra.children().isEmpty());
    }

    @Test
    void rRuleReferenceAnnotation_extendsRNode() {
        RRuleReferenceAnnotation rra = new RRuleReferenceAnnotation();
        assertInstanceOf(RNode.class, rra);
        assertFalse(RRootElement.class.isAssignableFrom(RRuleReferenceAnnotation.class));
    }

    // =========================================================================
    // RAnnotationPathExpression
    // =========================================================================

    @Test
    void rAnnotationPathExpression_defaultState() {
        RAnnotationPathExpression path = new RAnnotationPathExpression();
        assertNull(path.root());
        assertFalse(path.isRootItem());
        assertTrue(path.segments().isEmpty());
        assertTrue(path.children().isEmpty());
    }

    @Test
    void rAnnotationPathExpression_setRoot() {
        RAnnotationPathExpression path = new RAnnotationPathExpression();
        path.setRoot("trade");
        path.setRootIsItem(false);

        assertEquals("trade", path.root());
        assertFalse(path.isRootItem());
    }

    @Test
    void rAnnotationPathExpression_itemRoot() {
        RAnnotationPathExpression path = new RAnnotationPathExpression();
        path.setRoot("item");
        path.setRootIsItem(true);

        assertEquals("item", path.root());
        assertTrue(path.isRootItem());
    }

    @Test
    void rAnnotationPathExpression_addSegments() {
        RAnnotationPathExpression path = new RAnnotationPathExpression();
        path.setRoot("trade");
        path.setRootIsItem(false);

        RAnnotationPathSegment seg1 = new RAnnotationPathSegment();
        seg1.setName("partyRole");
        seg1.setDeep(false);

        RAnnotationPathSegment seg2 = new RAnnotationPathSegment();
        seg2.setName("role");
        seg2.setDeep(true);

        path.segments().add(seg1);
        path.segments().add(seg2);

        assertEquals(2, path.segments().size());
        assertEquals("partyRole", path.segments().get(0).name());
        assertFalse(path.segments().get(0).isDeep());
        assertEquals("role", path.segments().get(1).name());
        assertTrue(path.segments().get(1).isDeep());
    }

    @Test
    void rAnnotationPathExpression_childrenIncludesSegments() {
        RAnnotationPathExpression path = new RAnnotationPathExpression();
        path.setRoot("trade");
        path.setRootIsItem(false);

        RAnnotationPathSegment seg = new RAnnotationPathSegment();
        seg.setName("partyRole");
        seg.setDeep(false);
        path.segments().add(seg);

        List<? extends RNode> children = path.children();
        assertEquals(1, children.size());
        assertSame(seg, children.get(0));
    }

    @Test
    void rAnnotationPathExpression_extendsRNode() {
        RAnnotationPathExpression path = new RAnnotationPathExpression();
        assertInstanceOf(RNode.class, path);
        assertFalse(RRootElement.class.isAssignableFrom(RAnnotationPathExpression.class));
    }

    // =========================================================================
    // RAnnotationPathSegment
    // =========================================================================

    @Test
    void rAnnotationPathSegment_defaultState() {
        RAnnotationPathSegment seg = new RAnnotationPathSegment();
        assertNull(seg.name());
        assertFalse(seg.isDeep());
    }

    @Test
    void rAnnotationPathSegment_shallowSegment() {
        RAnnotationPathSegment seg = new RAnnotationPathSegment();
        seg.setName("partyRole");
        seg.setDeep(false);

        assertEquals("partyRole", seg.name());
        assertFalse(seg.isDeep());
    }

    @Test
    void rAnnotationPathSegment_deepSegment() {
        RAnnotationPathSegment seg = new RAnnotationPathSegment();
        seg.setName("role");
        seg.setDeep(true);

        assertEquals("role", seg.name());
        assertTrue(seg.isDeep());
    }

    @Test
    void rAnnotationPathSegment_isLeafNode() {
        RAnnotationPathSegment seg = new RAnnotationPathSegment();
        seg.setName("attr");
        seg.setDeep(false);
        assertTrue(seg.children().isEmpty());
    }

    @Test
    void rAnnotationPathSegment_extendsRNode() {
        RAnnotationPathSegment seg = new RAnnotationPathSegment();
        assertInstanceOf(RNode.class, seg);
        assertFalse(RRootElement.class.isAssignableFrom(RAnnotationPathSegment.class));
    }

    // =========================================================================
    // Integration: annotation on data type attribute
    // =========================================================================

    @Test
    void integration_attributeWithAnnotationRefs() {
        // Build an attribute with annotation refs to verify the typed list works
        RAttribute attr = new RAttribute();
        attr.setName("price");
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("number");
        attr.setTypeCall(tc);

        RAnnotationRef ref = new RAnnotationRef();
        ref.setAnnotationName("metadata");
        ref.setQualifierName("key");
        attr.annotationRefs().add(ref);

        assertEquals(1, attr.annotationRefs().size());
        assertEquals("metadata", attr.annotationRefs().get(0).annotationName());
        assertEquals(Optional.of("key"), attr.annotationRefs().get(0).qualifierName());

        // Verify it appears in children
        List<? extends RNode> children = attr.children();
        assertTrue(children.contains(ref));
    }

    @Test
    void integration_attributeWithLabelAndRuleReferenceAnnotations() {
        RAttribute attr = new RAttribute();
        attr.setName("counterparty");
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("Party");
        attr.setTypeCall(tc);

        // Add label annotation
        RLabelAnnotation label = new RLabelAnnotation();
        label.setLabel("Counterparty Name");
        attr.labelAnnotations().add(label);

        // Add rule reference annotation
        RRuleReferenceAnnotation ruleRef = new RRuleReferenceAnnotation();
        ruleRef.setRuleName("CounterpartyRule");
        attr.ruleReferenceAnnotations().add(ruleRef);

        assertEquals(1, attr.labelAnnotations().size());
        assertEquals("Counterparty Name", attr.labelAnnotations().get(0).label());
        assertEquals(1, attr.ruleReferenceAnnotations().size());
        assertEquals(Optional.of("CounterpartyRule"), attr.ruleReferenceAnnotations().get(0).ruleName());

        // All should appear in children
        List<? extends RNode> children = attr.children();
        assertTrue(children.contains(label));
        assertTrue(children.contains(ruleRef));
    }

    @Test
    void integration_fullAnnotationDeclaration() {
        // annotation metadata: [prefix myMeta]
        //     <"Metadata annotation">
        //     key string (0..1)
        //     scheme string (0..1)
        RAnnotation ann = new RAnnotation();
        ann.setName("metadata");
        ann.setPrefix("myMeta");
        ann.setDefinition("Metadata annotation");

        RAttribute attr1 = new RAttribute();
        attr1.setName("key");
        RTypeCall tc1 = new RTypeCall();
        tc1.setTypeName("string");
        attr1.setTypeCall(tc1);

        RAttribute attr2 = new RAttribute();
        attr2.setName("scheme");
        RTypeCall tc2 = new RTypeCall();
        tc2.setTypeName("string");
        attr2.setTypeCall(tc2);

        ann.attributes().add(attr1);
        ann.attributes().add(attr2);

        // Verify the full tree
        assertEquals("metadata", ann.name());
        assertEquals(Optional.of("myMeta"), ann.prefix());
        assertEquals(Optional.of("Metadata annotation"), ann.definition());
        assertEquals(2, ann.attributes().size());

        List<? extends RNode> children = ann.children();
        assertEquals(2, children.size());
        assertSame(attr1, children.get(0));
        assertSame(attr2, children.get(1));
    }

    @Test
    void integration_deepPathExpression() {
        // trade -> partyRole ->* role -> identifier
        RAnnotationPathExpression path = new RAnnotationPathExpression();
        path.setRoot("trade");
        path.setRootIsItem(false);

        RAnnotationPathSegment seg1 = new RAnnotationPathSegment();
        seg1.setName("partyRole");
        seg1.setDeep(false);

        RAnnotationPathSegment seg2 = new RAnnotationPathSegment();
        seg2.setName("role");
        seg2.setDeep(true);

        RAnnotationPathSegment seg3 = new RAnnotationPathSegment();
        seg3.setName("identifier");
        seg3.setDeep(false);

        path.segments().add(seg1);
        path.segments().add(seg2);
        path.segments().add(seg3);

        assertEquals("trade", path.root());
        assertFalse(path.isRootItem());
        assertEquals(3, path.segments().size());
        assertFalse(path.segments().get(0).isDeep());
        assertTrue(path.segments().get(1).isDeep());
        assertFalse(path.segments().get(2).isDeep());

        List<? extends RNode> children = path.children();
        assertEquals(3, children.size());
    }

    // =========================================================================
    // Integration: parse-and-build tests
    // =========================================================================

    @Nested
    class IntegrationParseAndBuild {

        @Test
        void parseAnnotationDeclMinimal() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "annotation rootType:");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RAnnotation.class, model.rootElements().get(0));

            RAnnotation ann = (RAnnotation) model.rootElements().get(0);
            assertEquals("rootType", ann.name());
            assertEquals(Optional.empty(), ann.definition());
            assertEquals(Optional.empty(), ann.prefix());
            assertTrue(ann.attributes().isEmpty());
            assertSourceRangeSet(ann);
        }

        @Test
        void parseAnnotationDeclWithDefinition() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "annotation metadata: <\"Describes metadata annotations.\">");
            RAnnotation ann = (RAnnotation) model.rootElements().get(0);
            assertEquals("metadata", ann.name());
            assertEquals(Optional.of("Describes metadata annotations."), ann.definition());
        }

        @Test
        void parseAnnotationDeclWithPrefix() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "annotation metadata: <\"Metadata.\">\n"
                    + "    [prefix myMeta]");
            RAnnotation ann = (RAnnotation) model.rootElements().get(0);
            assertEquals("metadata", ann.name());
            assertEquals(Optional.of("myMeta"), ann.prefix());
        }

        @Test
        void parseAnnotationDeclWithAttributes() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "annotation metadata: <\"Metadata.\">\n"
                    + "    key string (0..1)\n"
                    + "    scheme string (0..1)");
            RAnnotation ann = (RAnnotation) model.rootElements().get(0);
            assertEquals(2, ann.attributes().size());
            assertEquals("key", ann.attributes().get(0).name());
            assertEquals("string", ann.attributes().get(0).typeCall().typeName());
            assertEquals("scheme", ann.attributes().get(1).name());
        }

        @Test
        void parseAnnotationRefOnDataType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    [rootType]\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertEquals(1, dt.annotationRefs().size());

            RAnnotationRef ref = dt.annotationRefs().get(0);
            assertEquals("rootType", ref.annotationName());
            assertEquals(Optional.empty(), ref.qualifierName());
            assertTrue(ref.qualifiers().isEmpty());
            assertSourceRangeSet(ref);
        }

        @Test
        void parseAnnotationRefWithQualifierName() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [metadata key]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertEquals(1, attr.annotationRefs().size());

            RAnnotationRef ref = attr.annotationRefs().get(0);
            assertEquals("metadata", ref.annotationName());
            assertEquals(Optional.of("key"), ref.qualifierName());
        }

        @Test
        void parseAnnotationRefOnEnumeration() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "enum MyEnum:\n"
                    + "    [rootType]\n"
                    + "    VAL1");
            var en = (com.regnosys.rosetta.ast.types.REnumeration) model.rootElements().get(0);
            assertEquals(1, en.annotationRefs().size());
            assertEquals("rootType", en.annotationRefs().get(0).annotationName());
        }

        @Test
        void parseLabelAnnotationOnAttribute() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [label \"Bar Label\"]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertEquals(1, attr.labelAnnotations().size());

            RLabelAnnotation la = attr.labelAnnotations().get(0);
            assertEquals("Bar Label", la.label());
            assertEquals(Optional.empty(), la.forPath());
            assertEquals(Optional.empty(), la.asPath());
            assertSourceRangeSet(la);
        }

        @Test
        void parseRuleReferenceAnnotationOnAttribute() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [ruleReference MyRule]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertEquals(1, attr.ruleReferenceAnnotations().size());

            RRuleReferenceAnnotation rra = attr.ruleReferenceAnnotations().get(0);
            assertEquals(Optional.of("MyRule"), rra.ruleName());
            assertEquals(Optional.empty(), rra.forPath());
            assertSourceRangeSet(rra);
        }

        @Test
        void parseRuleReferenceAnnotationEmpty() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [ruleReference empty]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertEquals(1, attr.ruleReferenceAnnotations().size());

            RRuleReferenceAnnotation rra = attr.ruleReferenceAnnotations().get(0);
            assertEquals(Optional.empty(), rra.ruleName());
        }

        @Test
        void parseAnnotationRefOnChoice() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "choice MyChoice:\n"
                    + "    [rootType]\n"
                    + "    Foo\n"
                    + "    Bar");
            var ch = (com.regnosys.rosetta.ast.types.RChoice) model.rootElements().get(0);
            assertEquals(1, ch.annotationRefs().size());
            assertEquals("rootType", ch.annotationRefs().get(0).annotationName());
        }

        @Test
        void parseAnnotationQualifierWithAttributeReference() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "annotation myAnnotation:\n"
                    + "type Foo:\n"
                    + "    [myAnnotation config \"key\" = Foo -> bar]\n"
                    + "    bar string (1..1)");
            var dt = (com.regnosys.rosetta.ast.types.RDataType) model.rootElements().get(1);
            assertEquals(1, dt.annotationRefs().size());
            var ref = dt.annotationRefs().get(0);
            assertEquals("myAnnotation", ref.annotationName());
            assertTrue(ref.qualifierName().isPresent());
            assertEquals("config", ref.qualifierName().get());
            assertEquals(1, ref.qualifiers().size());
            var q = ref.qualifiers().get(0);
            assertEquals("key", q.key());
            assertTrue(q.isAttributeRef());
            assertEquals("Foo -> bar", q.value());
        }
    }
}
