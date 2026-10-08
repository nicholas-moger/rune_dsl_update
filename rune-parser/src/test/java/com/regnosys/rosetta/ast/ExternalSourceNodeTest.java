package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.annotations.RAnnotationPathExpression;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.enums.SynonymBodyKind;
import com.regnosys.rosetta.ast.external.RExternalClass;
import com.regnosys.rosetta.ast.external.RExternalClassSynonym;
import com.regnosys.rosetta.ast.external.RExternalEnum;
import com.regnosys.rosetta.ast.external.RExternalEnumSynonym;
import com.regnosys.rosetta.ast.external.RExternalEnumValue;
import com.regnosys.rosetta.ast.external.RExternalRegularAttribute;
import com.regnosys.rosetta.ast.external.RExternalRuleSource;
import com.regnosys.rosetta.ast.external.RExternalSynonym;
import com.regnosys.rosetta.ast.external.RExternalSynonymSource;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.synonyms.RClassSynonymValue;
import com.regnosys.rosetta.ast.synonyms.RMetaSynonymValue;
import com.regnosys.rosetta.ast.synonyms.RSynonymBody;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for external source AST nodes (Task 8).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, list fields, boolean getters, and the
 * {@code children()} traversal.
 */
class ExternalSourceNodeTest extends BaseAstTest {

    // =========================================================================
    // RExternalSynonymSource
    // =========================================================================

    @Test
    void rExternalSynonymSource_defaultState() {
        RExternalSynonymSource src = new RExternalSynonymSource();
        assertNull(src.name());
        assertTrue(src.superSourceNames().isEmpty());
        assertTrue(src.classes().isEmpty());
        assertTrue(src.enums().isEmpty());
        assertTrue(src.children().isEmpty());
    }

    @Test
    void rExternalSynonymSource_setName() {
        RExternalSynonymSource src = new RExternalSynonymSource();
        src.setName("FpML_5_10");
        assertEquals("FpML_5_10", src.name());
    }

    @Test
    void rExternalSynonymSource_superSourceNames() {
        RExternalSynonymSource src = new RExternalSynonymSource();
        src.setName("FpML_5_11");
        src.superSourceNames().add("FpML_5_10");
        src.superSourceNames().add("FpML_Base");

        assertEquals(2, src.superSourceNames().size());
        assertEquals("FpML_5_10", src.superSourceNames().get(0));
        assertEquals("FpML_Base", src.superSourceNames().get(1));
    }

    @Test
    void rExternalSynonymSource_classesAndEnums() {
        RExternalSynonymSource src = new RExternalSynonymSource();
        src.setName("FpML");

        RExternalClass cls = new RExternalClass();
        cls.setTypeName("Trade");
        src.classes().add(cls);

        RExternalEnum en = new RExternalEnum();
        en.setTypeName("TradeStatus");
        src.enums().add(en);

        assertEquals(1, src.classes().size());
        assertEquals(1, src.enums().size());
    }

    @Test
    void rExternalSynonymSource_children() {
        RExternalSynonymSource src = new RExternalSynonymSource();
        src.setName("FpML");

        RExternalClass cls = new RExternalClass();
        cls.setTypeName("Trade");
        src.classes().add(cls);

        RExternalEnum en = new RExternalEnum();
        en.setTypeName("TradeStatus");
        src.enums().add(en);

        List<? extends RNode> children = src.children();
        assertEquals(2, children.size());
        assertSame(cls, children.get(0));
        assertSame(en, children.get(1));
    }

    @Test
    void rExternalSynonymSource_extendsRRootElement() {
        RExternalSynonymSource src = new RExternalSynonymSource();
        assertInstanceOf(RRootElement.class, src);
        assertInstanceOf(RNode.class, src);
    }

    @Test
    void rExternalSynonymSource_doesNotImplementRDefinable() {
        assertFalse(RDefinable.class.isAssignableFrom(RExternalSynonymSource.class));
    }

    // =========================================================================
    // RExternalRuleSource
    // =========================================================================

    @Test
    void rExternalRuleSource_defaultState() {
        RExternalRuleSource src = new RExternalRuleSource();
        assertNull(src.name());
        assertTrue(src.superSourceNames().isEmpty());
        assertTrue(src.classes().isEmpty());
        assertTrue(src.enums().isEmpty());
        assertTrue(src.children().isEmpty());
    }

    @Test
    void rExternalRuleSource_setName() {
        RExternalRuleSource src = new RExternalRuleSource();
        src.setName("DRR_Rules");
        assertEquals("DRR_Rules", src.name());
    }

    @Test
    void rExternalRuleSource_superSourceNames() {
        RExternalRuleSource src = new RExternalRuleSource();
        src.setName("DRR_v2");
        src.superSourceNames().add("DRR_v1");

        assertEquals(1, src.superSourceNames().size());
        assertEquals("DRR_v1", src.superSourceNames().get(0));
    }

    @Test
    void rExternalRuleSource_classesAndEnums() {
        RExternalRuleSource src = new RExternalRuleSource();
        src.setName("DRR");

        RExternalClass cls = new RExternalClass();
        cls.setTypeName("Report");
        src.classes().add(cls);

        RExternalEnum en = new RExternalEnum();
        en.setTypeName("ReportStatus");
        src.enums().add(en);

        assertEquals(1, src.classes().size());
        assertEquals(1, src.enums().size());
    }

    @Test
    void rExternalRuleSource_children() {
        RExternalRuleSource src = new RExternalRuleSource();
        src.setName("DRR");

        RExternalClass cls1 = new RExternalClass();
        cls1.setTypeName("Report");
        RExternalClass cls2 = new RExternalClass();
        cls2.setTypeName("Trade");
        src.classes().add(cls1);
        src.classes().add(cls2);

        RExternalEnum en = new RExternalEnum();
        en.setTypeName("Status");
        src.enums().add(en);

        List<? extends RNode> children = src.children();
        assertEquals(3, children.size());
        assertSame(cls1, children.get(0));
        assertSame(cls2, children.get(1));
        assertSame(en, children.get(2));
    }

    @Test
    void rExternalRuleSource_extendsRRootElement() {
        RExternalRuleSource src = new RExternalRuleSource();
        assertInstanceOf(RRootElement.class, src);
        assertInstanceOf(RNode.class, src);
    }

    @Test
    void rExternalRuleSource_doesNotImplementRDefinable() {
        assertFalse(RDefinable.class.isAssignableFrom(RExternalRuleSource.class));
    }

    // =========================================================================
    // RExternalClass
    // =========================================================================

    @Test
    void rExternalClass_defaultState() {
        RExternalClass cls = new RExternalClass();
        assertNull(cls.typeName());
        assertTrue(cls.classSynonyms().isEmpty());
        assertTrue(cls.attributes().isEmpty());
        assertTrue(cls.children().isEmpty());
    }

    @Test
    void rExternalClass_setTypeName() {
        RExternalClass cls = new RExternalClass();
        cls.setTypeName("com.example.Trade");
        assertEquals("com.example.Trade", cls.typeName());
    }

    @Test
    void rExternalClass_classSynonymsAndAttributes() {
        RExternalClass cls = new RExternalClass();
        cls.setTypeName("Trade");

        RExternalClassSynonym cs = new RExternalClassSynonym();
        RMetaSynonymValue meta = new RMetaSynonymValue();
        meta.setName("tradeHeader");
        cs.setMeta(meta);
        cls.classSynonyms().add(cs);

        RExternalRegularAttribute attr = new RExternalRegularAttribute();
        attr.setAddition(true);
        attr.setName("price");
        cls.attributes().add(attr);

        assertEquals(1, cls.classSynonyms().size());
        assertEquals(1, cls.attributes().size());
    }

    @Test
    void rExternalClass_children() {
        RExternalClass cls = new RExternalClass();
        cls.setTypeName("Trade");

        RExternalClassSynonym cs = new RExternalClassSynonym();
        RMetaSynonymValue meta = new RMetaSynonymValue();
        meta.setName("header");
        cs.setMeta(meta);
        cls.classSynonyms().add(cs);

        RExternalRegularAttribute attr = new RExternalRegularAttribute();
        attr.setAddition(true);
        attr.setName("price");
        cls.attributes().add(attr);

        List<? extends RNode> children = cls.children();
        assertEquals(2, children.size());
        assertSame(cs, children.get(0));
        assertSame(attr, children.get(1));
    }

    @Test
    void rExternalClass_extendsRNode() {
        RExternalClass cls = new RExternalClass();
        assertInstanceOf(RNode.class, cls);
        assertFalse(RRootElement.class.isAssignableFrom(RExternalClass.class));
    }

    // =========================================================================
    // RExternalEnum
    // =========================================================================

    @Test
    void rExternalEnum_defaultState() {
        RExternalEnum en = new RExternalEnum();
        assertNull(en.typeName());
        assertTrue(en.values().isEmpty());
        assertTrue(en.children().isEmpty());
    }

    @Test
    void rExternalEnum_setTypeName() {
        RExternalEnum en = new RExternalEnum();
        en.setTypeName("TradeStatus");
        assertEquals("TradeStatus", en.typeName());
    }

    @Test
    void rExternalEnum_values() {
        RExternalEnum en = new RExternalEnum();
        en.setTypeName("TradeStatus");

        RExternalEnumValue val1 = new RExternalEnumValue();
        val1.setAddition(true);
        val1.setName("Active");
        en.values().add(val1);

        RExternalEnumValue val2 = new RExternalEnumValue();
        val2.setAddition(false);
        val2.setName("Cancelled");
        en.values().add(val2);

        assertEquals(2, en.values().size());
    }

    @Test
    void rExternalEnum_children() {
        RExternalEnum en = new RExternalEnum();
        en.setTypeName("Status");

        RExternalEnumValue val = new RExternalEnumValue();
        val.setAddition(true);
        val.setName("Active");
        en.values().add(val);

        List<? extends RNode> children = en.children();
        assertEquals(1, children.size());
        assertSame(val, children.get(0));
    }

    @Test
    void rExternalEnum_extendsRNode() {
        RExternalEnum en = new RExternalEnum();
        assertInstanceOf(RNode.class, en);
        assertFalse(RRootElement.class.isAssignableFrom(RExternalEnum.class));
    }

    // =========================================================================
    // RExternalRegularAttribute
    // =========================================================================

    @Test
    void rExternalRegularAttribute_defaultState() {
        RExternalRegularAttribute attr = new RExternalRegularAttribute();
        assertFalse(attr.isAddition());
        assertNull(attr.name());
        assertTrue(attr.synonyms().isEmpty());
        assertTrue(attr.ruleRefs().isEmpty());
        assertTrue(attr.children().isEmpty());
    }

    @Test
    void rExternalRegularAttribute_addition() {
        RExternalRegularAttribute attr = new RExternalRegularAttribute();
        attr.setAddition(true);
        attr.setName("price");
        assertTrue(attr.isAddition());
        assertEquals("price", attr.name());
    }

    @Test
    void rExternalRegularAttribute_removal() {
        RExternalRegularAttribute attr = new RExternalRegularAttribute();
        attr.setAddition(false);
        attr.setName("deprecated");
        assertFalse(attr.isAddition());
        assertEquals("deprecated", attr.name());
    }

    @Test
    void rExternalRegularAttribute_synonymsAndRuleRefs() {
        RExternalRegularAttribute attr = new RExternalRegularAttribute();
        attr.setAddition(true);
        attr.setName("price");

        RExternalSynonym syn = new RExternalSynonym();
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.VALUE);
        syn.setBody(body);
        attr.synonyms().add(syn);

        RRuleReferenceAnnotation ruleRef = new RRuleReferenceAnnotation();
        ruleRef.setRuleName("PriceRule");
        attr.ruleRefs().add(ruleRef);

        assertEquals(1, attr.synonyms().size());
        assertEquals(1, attr.ruleRefs().size());
    }

    @Test
    void rExternalRegularAttribute_children() {
        RExternalRegularAttribute attr = new RExternalRegularAttribute();
        attr.setAddition(true);
        attr.setName("price");

        RExternalSynonym syn = new RExternalSynonym();
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.VALUE);
        syn.setBody(body);
        attr.synonyms().add(syn);

        RRuleReferenceAnnotation ruleRef = new RRuleReferenceAnnotation();
        ruleRef.setRuleName("PriceRule");
        attr.ruleRefs().add(ruleRef);

        List<? extends RNode> children = attr.children();
        assertEquals(2, children.size());
        assertSame(syn, children.get(0));
        assertSame(ruleRef, children.get(1));
    }

    @Test
    void rExternalRegularAttribute_extendsRNode() {
        RExternalRegularAttribute attr = new RExternalRegularAttribute();
        assertInstanceOf(RNode.class, attr);
        assertFalse(RRootElement.class.isAssignableFrom(RExternalRegularAttribute.class));
    }

    // =========================================================================
    // RExternalEnumValue
    // =========================================================================

    @Test
    void rExternalEnumValue_defaultState() {
        RExternalEnumValue val = new RExternalEnumValue();
        assertFalse(val.isAddition());
        assertNull(val.name());
        assertTrue(val.synonyms().isEmpty());
        assertTrue(val.children().isEmpty());
    }

    @Test
    void rExternalEnumValue_addition() {
        RExternalEnumValue val = new RExternalEnumValue();
        val.setAddition(true);
        val.setName("Active");
        assertTrue(val.isAddition());
        assertEquals("Active", val.name());
    }

    @Test
    void rExternalEnumValue_removal() {
        RExternalEnumValue val = new RExternalEnumValue();
        val.setAddition(false);
        val.setName("Deprecated");
        assertFalse(val.isAddition());
        assertEquals("Deprecated", val.name());
    }

    @Test
    void rExternalEnumValue_synonyms() {
        RExternalEnumValue val = new RExternalEnumValue();
        val.setAddition(true);
        val.setName("Active");

        RExternalEnumSynonym syn = new RExternalEnumSynonym();
        syn.setValue("ActiveTrade");
        val.synonyms().add(syn);

        assertEquals(1, val.synonyms().size());
        assertEquals("ActiveTrade", val.synonyms().get(0).value());
    }

    @Test
    void rExternalEnumValue_children() {
        RExternalEnumValue val = new RExternalEnumValue();
        val.setAddition(true);
        val.setName("Active");

        RExternalEnumSynonym syn = new RExternalEnumSynonym();
        syn.setValue("ActiveTrade");
        val.synonyms().add(syn);

        List<? extends RNode> children = val.children();
        assertEquals(1, children.size());
        assertSame(syn, children.get(0));
    }

    @Test
    void rExternalEnumValue_extendsRNode() {
        RExternalEnumValue val = new RExternalEnumValue();
        assertInstanceOf(RNode.class, val);
        assertFalse(RRootElement.class.isAssignableFrom(RExternalEnumValue.class));
    }

    // =========================================================================
    // RExternalClassSynonym
    // =========================================================================

    @Test
    void rExternalClassSynonym_defaultState() {
        RExternalClassSynonym cs = new RExternalClassSynonym();
        assertEquals(Optional.empty(), cs.value());
        assertNull(cs.meta());
        assertTrue(cs.children().isEmpty());
    }

    @Test
    void rExternalClassSynonym_withValueAndMeta() {
        RExternalClassSynonym cs = new RExternalClassSynonym();

        RClassSynonymValue value = new RClassSynonymValue();
        value.setName("Trade");
        cs.setValue(value);

        RMetaSynonymValue meta = new RMetaSynonymValue();
        meta.setName("tradeHeader");
        cs.setMeta(meta);

        assertTrue(cs.value().isPresent());
        assertEquals("Trade", cs.value().get().name());
        assertNotNull(cs.meta());
        assertEquals("tradeHeader", cs.meta().name());
    }

    @Test
    void rExternalClassSynonym_metaOnly() {
        RExternalClassSynonym cs = new RExternalClassSynonym();

        RMetaSynonymValue meta = new RMetaSynonymValue();
        meta.setName("scheme");
        cs.setMeta(meta);

        assertEquals(Optional.empty(), cs.value());
        assertEquals("scheme", cs.meta().name());
    }

    @Test
    void rExternalClassSynonym_childrenWithValueAndMeta() {
        RExternalClassSynonym cs = new RExternalClassSynonym();

        RClassSynonymValue value = new RClassSynonymValue();
        value.setName("Trade");
        cs.setValue(value);

        RMetaSynonymValue meta = new RMetaSynonymValue();
        meta.setName("header");
        cs.setMeta(meta);

        List<? extends RNode> children = cs.children();
        assertEquals(2, children.size());
        assertSame(value, children.get(0));
        assertSame(meta, children.get(1));
    }

    @Test
    void rExternalClassSynonym_childrenWithMetaOnly() {
        RExternalClassSynonym cs = new RExternalClassSynonym();

        RMetaSynonymValue meta = new RMetaSynonymValue();
        meta.setName("scheme");
        cs.setMeta(meta);

        List<? extends RNode> children = cs.children();
        assertEquals(1, children.size());
        assertSame(meta, children.get(0));
    }

    @Test
    void rExternalClassSynonym_extendsRNode() {
        RExternalClassSynonym cs = new RExternalClassSynonym();
        assertInstanceOf(RNode.class, cs);
        assertFalse(RRootElement.class.isAssignableFrom(RExternalClassSynonym.class));
    }

    // =========================================================================
    // RExternalSynonym
    // =========================================================================

    @Test
    void rExternalSynonym_defaultState() {
        RExternalSynonym syn = new RExternalSynonym();
        assertNull(syn.body());
        assertTrue(syn.children().isEmpty());
    }

    @Test
    void rExternalSynonym_setBody() {
        RExternalSynonym syn = new RExternalSynonym();
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.VALUE);
        syn.setBody(body);

        assertSame(body, syn.body());
    }

    @Test
    void rExternalSynonym_children() {
        RExternalSynonym syn = new RExternalSynonym();
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.HINT);
        syn.setBody(body);

        List<? extends RNode> children = syn.children();
        assertEquals(1, children.size());
        assertSame(body, children.get(0));
    }

    @Test
    void rExternalSynonym_childrenEmptyWithoutBody() {
        RExternalSynonym syn = new RExternalSynonym();
        assertTrue(syn.children().isEmpty());
    }

    @Test
    void rExternalSynonym_extendsRNode() {
        RExternalSynonym syn = new RExternalSynonym();
        assertInstanceOf(RNode.class, syn);
        assertFalse(RRootElement.class.isAssignableFrom(RExternalSynonym.class));
    }

    // =========================================================================
    // RExternalEnumSynonym
    // =========================================================================

    @Test
    void rExternalEnumSynonym_defaultState() {
        RExternalEnumSynonym syn = new RExternalEnumSynonym();
        assertNull(syn.value());
        assertEquals(Optional.empty(), syn.definitionText());
        assertEquals(Optional.empty(), syn.patternMatch());
        assertEquals(Optional.empty(), syn.patternReplace());
    }

    @Test
    void rExternalEnumSynonym_setValue() {
        RExternalEnumSynonym syn = new RExternalEnumSynonym();
        syn.setValue("ActiveTrade");
        assertEquals("ActiveTrade", syn.value());
    }

    @Test
    void rExternalEnumSynonym_allFields() {
        RExternalEnumSynonym syn = new RExternalEnumSynonym();
        syn.setValue("ActiveTrade");
        syn.setDefinitionText("An active trade");
        syn.setPatternMatch("Active.*");
        syn.setPatternReplace("Active");

        assertEquals("ActiveTrade", syn.value());
        assertEquals(Optional.of("An active trade"), syn.definitionText());
        assertEquals(Optional.of("Active.*"), syn.patternMatch());
        assertEquals(Optional.of("Active"), syn.patternReplace());
    }

    @Test
    void rExternalEnumSynonym_minimalFields() {
        RExternalEnumSynonym syn = new RExternalEnumSynonym();
        syn.setValue("Active");

        assertEquals("Active", syn.value());
        assertEquals(Optional.empty(), syn.definitionText());
        assertEquals(Optional.empty(), syn.patternMatch());
        assertEquals(Optional.empty(), syn.patternReplace());
    }

    @Test
    void rExternalEnumSynonym_isLeafNode() {
        RExternalEnumSynonym syn = new RExternalEnumSynonym();
        syn.setValue("Active");
        assertTrue(syn.children().isEmpty());
    }

    @Test
    void rExternalEnumSynonym_extendsRNode() {
        RExternalEnumSynonym syn = new RExternalEnumSynonym();
        assertInstanceOf(RNode.class, syn);
        assertFalse(RRootElement.class.isAssignableFrom(RExternalEnumSynonym.class));
    }

    // =========================================================================
    // Integration: full external synonym source tree
    // =========================================================================

    @Test
    void integration_fullExternalSynonymSourceTree() {
        // Build a full external synonym source tree:
        //
        // synonym source FpML_5_11 extends FpML_5_10 {
        //     Trade:
        //         [synonym value "Trade" meta "tradeHeader"]
        //         + price:
        //             [synonym value "tradePrice"]
        //         - deprecated:
        //     enums
        //     TradeStatus:
        //         + Active:
        //             [synonym "ActiveTrade" definition "An active trade"
        //                      pattern "Active.*" "Active"]
        //         - Cancelled:
        // }

        RExternalSynonymSource source = new RExternalSynonymSource();
        source.setName("FpML_5_11");
        source.superSourceNames().add("FpML_5_10");

        // -- External class: Trade
        RExternalClass tradeClass = new RExternalClass();
        tradeClass.setTypeName("Trade");

        // Class synonym
        RExternalClassSynonym classSyn = new RExternalClassSynonym();
        RClassSynonymValue csvValue = new RClassSynonymValue();
        csvValue.setName("Trade");
        classSyn.setValue(csvValue);
        RMetaSynonymValue metaValue = new RMetaSynonymValue();
        metaValue.setName("tradeHeader");
        classSyn.setMeta(metaValue);
        tradeClass.classSynonyms().add(classSyn);

        // Attribute: + price
        RExternalRegularAttribute priceAttr = new RExternalRegularAttribute();
        priceAttr.setAddition(true);
        priceAttr.setName("price");

        RExternalSynonym priceSyn = new RExternalSynonym();
        RSynonymBody priceBody = new RSynonymBody();
        priceBody.setKind(SynonymBodyKind.VALUE);
        priceSyn.setBody(priceBody);
        priceAttr.synonyms().add(priceSyn);

        tradeClass.attributes().add(priceAttr);

        // Attribute: - deprecated (no synonyms, no rule refs)
        RExternalRegularAttribute deprecatedAttr = new RExternalRegularAttribute();
        deprecatedAttr.setAddition(false);
        deprecatedAttr.setName("deprecated");
        tradeClass.attributes().add(deprecatedAttr);

        source.classes().add(tradeClass);

        // -- External enum: TradeStatus
        RExternalEnum statusEnum = new RExternalEnum();
        statusEnum.setTypeName("TradeStatus");

        // + Active
        RExternalEnumValue activeVal = new RExternalEnumValue();
        activeVal.setAddition(true);
        activeVal.setName("Active");

        RExternalEnumSynonym activeSyn = new RExternalEnumSynonym();
        activeSyn.setValue("ActiveTrade");
        activeSyn.setDefinitionText("An active trade");
        activeSyn.setPatternMatch("Active.*");
        activeSyn.setPatternReplace("Active");
        activeVal.synonyms().add(activeSyn);

        statusEnum.values().add(activeVal);

        // - Cancelled (no synonyms)
        RExternalEnumValue cancelledVal = new RExternalEnumValue();
        cancelledVal.setAddition(false);
        cancelledVal.setName("Cancelled");
        statusEnum.values().add(cancelledVal);

        source.enums().add(statusEnum);

        // -- Verify the tree structure

        // Root level
        assertEquals("FpML_5_11", source.name());
        assertEquals(1, source.superSourceNames().size());
        assertEquals("FpML_5_10", source.superSourceNames().get(0));
        assertEquals(1, source.classes().size());
        assertEquals(1, source.enums().size());

        // Root children: 1 class + 1 enum
        List<? extends RNode> rootChildren = source.children();
        assertEquals(2, rootChildren.size());
        assertSame(tradeClass, rootChildren.get(0));
        assertSame(statusEnum, rootChildren.get(1));

        // Trade class
        assertEquals("Trade", tradeClass.typeName());
        assertEquals(1, tradeClass.classSynonyms().size());
        assertEquals(2, tradeClass.attributes().size());

        // Trade class children: 1 classSynonym + 2 attributes
        List<? extends RNode> tradeChildren = tradeClass.children();
        assertEquals(3, tradeChildren.size());
        assertSame(classSyn, tradeChildren.get(0));
        assertSame(priceAttr, tradeChildren.get(1));
        assertSame(deprecatedAttr, tradeChildren.get(2));

        // Class synonym
        assertTrue(classSyn.value().isPresent());
        assertEquals("Trade", classSyn.value().get().name());
        assertEquals("tradeHeader", classSyn.meta().name());

        // Price attribute
        assertTrue(priceAttr.isAddition());
        assertEquals("price", priceAttr.name());
        assertEquals(1, priceAttr.synonyms().size());
        assertTrue(priceAttr.ruleRefs().isEmpty());

        // Deprecated attribute
        assertFalse(deprecatedAttr.isAddition());
        assertEquals("deprecated", deprecatedAttr.name());
        assertTrue(deprecatedAttr.synonyms().isEmpty());
        assertTrue(deprecatedAttr.children().isEmpty());

        // TradeStatus enum
        assertEquals("TradeStatus", statusEnum.typeName());
        assertEquals(2, statusEnum.values().size());

        // Active value
        assertTrue(activeVal.isAddition());
        assertEquals("Active", activeVal.name());
        assertEquals(1, activeVal.synonyms().size());

        RExternalEnumSynonym activeSynActual = activeVal.synonyms().get(0);
        assertEquals("ActiveTrade", activeSynActual.value());
        assertEquals(Optional.of("An active trade"), activeSynActual.definitionText());
        assertEquals(Optional.of("Active.*"), activeSynActual.patternMatch());
        assertEquals(Optional.of("Active"), activeSynActual.patternReplace());

        // Active value children: 1 synonym
        List<? extends RNode> activeChildren = activeVal.children();
        assertEquals(1, activeChildren.size());
        assertSame(activeSyn, activeChildren.get(0));

        // Cancelled value (removal, no synonyms)
        assertFalse(cancelledVal.isAddition());
        assertEquals("Cancelled", cancelledVal.name());
        assertTrue(cancelledVal.synonyms().isEmpty());
        assertTrue(cancelledVal.children().isEmpty());
    }

    @Test
    void integration_externalRuleSourceWithRuleRefs() {
        // Build an external rule source with rule references:
        //
        // rule source DRR_Rules {
        //     Report:
        //         + tradeId:
        //             [ruleReference TradeIdRule]
        // }

        RExternalRuleSource ruleSource = new RExternalRuleSource();
        ruleSource.setName("DRR_Rules");

        RExternalClass reportClass = new RExternalClass();
        reportClass.setTypeName("Report");

        RExternalRegularAttribute tradeIdAttr = new RExternalRegularAttribute();
        tradeIdAttr.setAddition(true);
        tradeIdAttr.setName("tradeId");

        RRuleReferenceAnnotation ruleRef = new RRuleReferenceAnnotation();
        ruleRef.setRuleName("TradeIdRule");
        tradeIdAttr.ruleRefs().add(ruleRef);

        reportClass.attributes().add(tradeIdAttr);
        ruleSource.classes().add(reportClass);

        // Verify
        assertInstanceOf(RRootElement.class, ruleSource);
        assertEquals("DRR_Rules", ruleSource.name());
        assertEquals(1, ruleSource.classes().size());
        assertTrue(ruleSource.enums().isEmpty());

        List<? extends RNode> ruleChildren = ruleSource.children();
        assertEquals(1, ruleChildren.size());
        assertSame(reportClass, ruleChildren.get(0));

        // Attribute with rule ref
        RExternalRegularAttribute attr = reportClass.attributes().get(0);
        assertTrue(attr.isAddition());
        assertEquals("tradeId", attr.name());
        assertTrue(attr.synonyms().isEmpty());
        assertEquals(1, attr.ruleRefs().size());
        assertEquals(Optional.of("TradeIdRule"), attr.ruleRefs().get(0).ruleName());

        // Attribute children: 1 rule ref
        List<? extends RNode> attrChildren = attr.children();
        assertEquals(1, attrChildren.size());
        assertSame(ruleRef, attrChildren.get(0));
    }

    // =========================================================================
    // Integration: end-to-end parse-and-build tests (Task 14)
    // =========================================================================

    @Nested
    class IntegrationParseAndBuild {

        // -- External synonym source ----------------------------------------

        @Test
        void parseExternalSynonymSource_minimal() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "synonym source FpML {\n"
                    + "}");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RExternalSynonymSource.class, model.rootElements().get(0));

            RExternalSynonymSource src = (RExternalSynonymSource) model.rootElements().get(0);
            assertEquals("FpML", src.name());
            assertTrue(src.superSourceNames().isEmpty());
            assertTrue(src.classes().isEmpty());
            assertTrue(src.enums().isEmpty());
            assertSourceRangeSet(src);
            assertSame(model, src.parent());
        }

        @Test
        void parseExternalSynonymSource_withExtends() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "synonym source FpML extends FpML_Base, DTCC {\n"
                    + "}");
            RExternalSynonymSource src = (RExternalSynonymSource) model.rootElements().get(0);
            assertEquals("FpML", src.name());
            assertEquals(List.of("FpML_Base", "DTCC"), src.superSourceNames());
        }

        @Test
        void parseExternalSynonymSource_withClassAndAttribute() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "synonym source FpML extends FpML_Base {\n"
                    + "    Foo:\n"
                    + "        + bar\n"
                    + "            [value \"barField\"]\n"
                    + "}");
            RExternalSynonymSource src = (RExternalSynonymSource) model.rootElements().get(0);
            assertEquals(1, src.classes().size());

            RExternalClass ec = src.classes().get(0);
            assertEquals("Foo", ec.typeName());
            assertEquals(1, ec.attributes().size());
            assertSourceRangeSet(ec);
            assertSame(src, ec.parent());

            RExternalRegularAttribute attr = ec.attributes().get(0);
            assertTrue(attr.isAddition());
            assertEquals("bar", attr.name());
            assertEquals(1, attr.synonyms().size());
            assertSourceRangeSet(attr);
            assertSame(ec, attr.parent());

            RExternalSynonym syn = attr.synonyms().get(0);
            assertNotNull(syn.body());
            assertEquals(SynonymBodyKind.VALUE, syn.body().kind());
            assertEquals(1, syn.body().values().size());
            assertEquals("barField", syn.body().values().get(0).name());
        }

        @Test
        void parseExternalSynonymSource_additionAndRemoval() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "synonym source FpML {\n"
                    + "    Foo:\n"
                    + "        + bar\n"
                    + "        - baz\n"
                    + "}");
            RExternalSynonymSource src = (RExternalSynonymSource) model.rootElements().get(0);
            RExternalClass ec = src.classes().get(0);

            assertEquals(2, ec.attributes().size());
            assertTrue(ec.attributes().get(0).isAddition());
            assertEquals("bar", ec.attributes().get(0).name());
            assertFalse(ec.attributes().get(1).isAddition());
            assertEquals("baz", ec.attributes().get(1).name());
        }

        @Test
        void parseExternalSynonymSource_classSynonym() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "synonym source FpML {\n"
                    + "    Foo:\n"
                    + "        [value \"FooClass\" meta \"id\"]\n"
                    + "        + bar\n"
                    + "}");
            RExternalSynonymSource src = (RExternalSynonymSource) model.rootElements().get(0);
            RExternalClass ec = src.classes().get(0);

            assertEquals(1, ec.classSynonyms().size());
            RExternalClassSynonym ecs = ec.classSynonyms().get(0);
            assertTrue(ecs.value().isPresent());
            assertEquals("FooClass", ecs.value().get().name());
            assertNotNull(ecs.meta());
            assertEquals("id", ecs.meta().name());
            assertSourceRangeSet(ecs);
            assertSame(ec, ecs.parent());
        }

        @Test
        void parseExternalSynonymSource_withEnums() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "synonym source FpML {\n"
                    + "    enums\n"
                    + "    Direction:\n"
                    + "        + North\n"
                    + "            [value \"N\"]\n"
                    + "        - SouthEast\n"
                    + "}");
            RExternalSynonymSource src = (RExternalSynonymSource) model.rootElements().get(0);
            assertEquals(1, src.enums().size());

            RExternalEnum ee = src.enums().get(0);
            assertEquals("Direction", ee.typeName());
            assertEquals(2, ee.values().size());
            assertSourceRangeSet(ee);
            assertSame(src, ee.parent());

            RExternalEnumValue north = ee.values().get(0);
            assertTrue(north.isAddition());
            assertEquals("North", north.name());
            assertEquals(1, north.synonyms().size());
            assertEquals("N", north.synonyms().get(0).value());
            assertSame(ee, north.parent());

            RExternalEnumValue southEast = ee.values().get(1);
            assertFalse(southEast.isAddition());
            assertEquals("SouthEast", southEast.name());
        }

        @Test
        void parseExternalSynonymSource_enumSynonymWithDefinitionAndPattern() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "synonym source FpML {\n"
                    + "    enums\n"
                    + "    Direction:\n"
                    + "        + North\n"
                    + "            [value \"N\" definition \"North\" pattern \"N.*\" \"N\"]\n"
                    + "}");
            RExternalSynonymSource src = (RExternalSynonymSource) model.rootElements().get(0);
            RExternalEnumSynonym es = src.enums().get(0).values().get(0).synonyms().get(0);

            assertEquals("N", es.value());
            assertEquals(Optional.of("North"), es.definitionText());
            assertEquals(Optional.of("N.*"), es.patternMatch());
            assertEquals(Optional.of("N"), es.patternReplace());
            assertSourceRangeSet(es);
        }

        // -- External rule source --------------------------------------------

        @Test
        void parseExternalRuleSource_minimal() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "rule source MyRules {\n"
                    + "}");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RExternalRuleSource.class, model.rootElements().get(0));

            RExternalRuleSource src = (RExternalRuleSource) model.rootElements().get(0);
            assertEquals("MyRules", src.name());
            assertTrue(src.classes().isEmpty());
            assertSourceRangeSet(src);
        }

        @Test
        void parseExternalRuleSource_withRuleReferences() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "rule source MyRules {\n"
                    + "    Foo:\n"
                    + "        + bar\n"
                    + "            [ruleReference TradeIdRule]\n"
                    + "}");
            RExternalRuleSource src = (RExternalRuleSource) model.rootElements().get(0);
            assertEquals(1, src.classes().size());
            RExternalClass ec = src.classes().get(0);
            assertEquals("Foo", ec.typeName());

            RExternalRegularAttribute attr = ec.attributes().get(0);
            assertTrue(attr.isAddition());
            assertEquals("bar", attr.name());
            assertEquals(1, attr.ruleRefs().size());
            assertEquals(Optional.of("TradeIdRule"), attr.ruleRefs().get(0).ruleName());
            assertSame(attr, attr.ruleRefs().get(0).parent());
        }

        @Test
        void parseExternalRuleSource_withExtends() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "rule source ChildRules extends BaseRules {\n"
                    + "}");
            RExternalRuleSource src = (RExternalRuleSource) model.rootElements().get(0);
            assertEquals(List.of("BaseRules"), src.superSourceNames());
        }

        // -- Multiple sources in one file ------------------------------------

        @Test
        void parseMultipleExternalSources() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "synonym source FpML {\n"
                    + "    Foo:\n"
                    + "        + bar\n"
                    + "}\n"
                    + "rule source MyRules {\n"
                    + "    Bar:\n"
                    + "        + baz\n"
                    + "}");
            assertEquals(2, model.rootElements().size());
            assertInstanceOf(RExternalSynonymSource.class, model.rootElements().get(0));
            assertInstanceOf(RExternalRuleSource.class, model.rootElements().get(1));
        }

        // -- Parent/source-range verification --------------------------------

        @Test
        void externalNodes_haveSourceRangesAndParents() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "synonym source FpML {\n"
                    + "    Foo:\n"
                    + "        [value \"FooClass\" meta \"id\"]\n"
                    + "        + bar\n"
                    + "            [value \"barField\"]\n"
                    + "    enums\n"
                    + "    Direction:\n"
                    + "        + North\n"
                    + "            [value \"N\"]\n"
                    + "}");
            RExternalSynonymSource src = (RExternalSynonymSource) model.rootElements().get(0);

            assertSourceRangeSet(src);
            assertSame(model, src.parent());

            RExternalClass ec = src.classes().get(0);
            assertSourceRangeSet(ec);
            assertSame(src, ec.parent());

            RExternalClassSynonym ecs = ec.classSynonyms().get(0);
            assertSourceRangeSet(ecs);
            assertSame(ec, ecs.parent());

            RExternalRegularAttribute attr = ec.attributes().get(0);
            assertSourceRangeSet(attr);
            assertSame(ec, attr.parent());

            RExternalSynonym es = attr.synonyms().get(0);
            assertSourceRangeSet(es);
            assertSame(attr, es.parent());
            assertSourceRangeSet(es.body());
            assertSame(es, es.body().parent());

            RExternalEnum ee = src.enums().get(0);
            assertSourceRangeSet(ee);
            assertSame(src, ee.parent());

            RExternalEnumValue ev = ee.values().get(0);
            assertSourceRangeSet(ev);
            assertSame(ee, ev.parent());

            RExternalEnumSynonym ees = ev.synonyms().get(0);
            assertSourceRangeSet(ees);
            assertSame(ev, ees.parent());
        }
    }
}
