package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.enums.RSynonymRef;
import com.regnosys.rosetta.ast.enums.SynonymBodyKind;
import com.regnosys.rosetta.ast.mapping.RMapping;
import com.regnosys.rosetta.ast.mapping.RMappingInstance;
import com.regnosys.rosetta.ast.mapping.RMappingSetTo;
import com.regnosys.rosetta.ast.mapping.RMappingSetToInstance;
import com.regnosys.rosetta.ast.mapping.RMapPrimaryExpression;
import com.regnosys.rosetta.ast.enums.MapPrimaryKind;
import com.regnosys.rosetta.ast.enums.MappingInstanceKind;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.synonyms.RClassSynonym;
import com.regnosys.rosetta.ast.synonyms.RClassSynonymValue;
import com.regnosys.rosetta.ast.synonyms.REnumSynonym;
import com.regnosys.rosetta.ast.synonyms.RMergeSynonymValue;
import com.regnosys.rosetta.ast.synonyms.RMetaSynonymValue;
import com.regnosys.rosetta.ast.synonyms.RSynonym;
import com.regnosys.rosetta.ast.synonyms.RSynonymBody;
import com.regnosys.rosetta.ast.synonyms.RSynonymSource;
import com.regnosys.rosetta.ast.synonyms.RSynonymValue;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.model.RModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for synonym-related AST nodes (Task 7).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, list fields, boolean getters, and the
 * {@code children()} traversal.
 */
class SynonymNodeTest extends BaseAstTest {

    // =========================================================================
    // RSynonymSource
    // =========================================================================

    @Test
    void rSynonymSource_defaultState() {
        RSynonymSource src = new RSynonymSource();
        assertNull(src.name());
        assertTrue(src.children().isEmpty());
    }

    @Test
    void rSynonymSource_setName() {
        RSynonymSource src = new RSynonymSource();
        src.setName("FpML");
        assertEquals("FpML", src.name());
    }

    @Test
    void rSynonymSource_extendsRRootElement() {
        RSynonymSource src = new RSynonymSource();
        assertInstanceOf(RRootElement.class, src);
        assertInstanceOf(RNode.class, src);
    }

    // =========================================================================
    // RSynonym
    // =========================================================================

    @Test
    void rSynonym_defaultState() {
        RSynonym syn = new RSynonym();
        assertTrue(syn.sources().isEmpty());
        assertNull(syn.body());
        assertTrue(syn.children().isEmpty());
    }

    @Test
    void rSynonym_addSources() {
        RSynonym syn = new RSynonym();
        syn.sources().add("FpML");
        syn.sources().add("DTCC");
        assertEquals(2, syn.sources().size());
        assertEquals("FpML", syn.sources().get(0));
        assertEquals("DTCC", syn.sources().get(1));
    }

    @Test
    void rSynonym_setBody() {
        RSynonym syn = new RSynonym();
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.VALUE);
        syn.setBody(body);

        assertSame(body, syn.body());
    }

    @Test
    void rSynonym_childrenIncludesBody() {
        RSynonym syn = new RSynonym();
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.HINT);
        syn.setBody(body);

        List<? extends RNode> children = syn.children();
        assertEquals(1, children.size());
        assertSame(body, children.get(0));
    }

    @Test
    void rSynonym_childrenEmptyWithoutBody() {
        RSynonym syn = new RSynonym();
        assertTrue(syn.children().isEmpty());
    }

    @Test
    void rSynonym_extendsRNode() {
        RSynonym syn = new RSynonym();
        assertInstanceOf(RNode.class, syn);
        assertFalse(RRootElement.class.isAssignableFrom(RSynonym.class));
    }

    // =========================================================================
    // RSynonymBody
    // =========================================================================

    @Test
    void rSynonymBody_defaultState() {
        RSynonymBody body = new RSynonymBody();
        assertNull(body.kind());
        assertTrue(body.values().isEmpty());
        assertEquals(Optional.empty(), body.mapping());
        assertTrue(body.metaFields().isEmpty());
        assertTrue(body.hints().isEmpty());
        assertEquals(Optional.empty(), body.merge());
        assertEquals(Optional.empty(), body.setTo());
        assertEquals(Optional.empty(), body.dateFormat());
        assertEquals(Optional.empty(), body.patternMatch());
        assertEquals(Optional.empty(), body.patternReplace());
        assertFalse(body.isRemoveHtml());
        assertEquals(Optional.empty(), body.mapper());
    }

    @Test
    void rSynonymBody_valueAlternative() {
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.VALUE);

        RSynonymValue val = new RSynonymValue();
        val.setName("tradeId");
        body.values().add(val);

        body.metaFields().add("scheme");

        assertEquals(SynonymBodyKind.VALUE, body.kind());
        assertEquals(1, body.values().size());
        assertEquals("tradeId", body.values().get(0).name());
        assertEquals(1, body.metaFields().size());
        assertEquals("scheme", body.metaFields().get(0));
    }

    @Test
    void rSynonymBody_hintAlternative() {
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.HINT);
        body.hints().add("partyReference");
        body.hints().add("accountReference");

        assertEquals(SynonymBodyKind.HINT, body.kind());
        assertEquals(2, body.hints().size());
    }

    @Test
    void rSynonymBody_mergeAlternative() {
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.MERGE);

        RMergeSynonymValue merge = new RMergeSynonymValue();
        merge.setName("trade");
        merge.setExcludePath("excluded");
        body.setMerge(merge);

        assertEquals(SynonymBodyKind.MERGE, body.kind());
        assertTrue(body.merge().isPresent());
        assertEquals("trade", body.merge().get().name());
    }

    @Test
    void rSynonymBody_setToAlternative() {
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.SET_TO);

        RMappingSetTo setTo = new RMappingSetTo();
        body.setSetTo(setTo);

        assertEquals(SynonymBodyKind.SET_TO, body.kind());
        assertTrue(body.setTo().isPresent());
    }

    @Test
    void rSynonymBody_metaOnlyAlternative() {
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.META_ONLY);
        body.metaFields().add("scheme");
        body.metaFields().add("id");

        assertEquals(SynonymBodyKind.META_ONLY, body.kind());
        assertEquals(2, body.metaFields().size());
    }

    @Test
    void rSynonymBody_trailingModifiers() {
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.VALUE);
        body.setDateFormat("yyyy-MM-dd");
        body.setPatternMatch("Active.*");
        body.setPatternReplace("Active");
        body.setRemoveHtml(true);
        body.setMapper("com.example.MyMapper");

        assertEquals(Optional.of("yyyy-MM-dd"), body.dateFormat());
        assertEquals(Optional.of("Active.*"), body.patternMatch());
        assertEquals(Optional.of("Active"), body.patternReplace());
        assertTrue(body.isRemoveHtml());
        assertEquals(Optional.of("com.example.MyMapper"), body.mapper());
    }

    @Test
    void rSynonymBody_childrenForValueWithMapping() {
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.VALUE);

        RSynonymValue val = new RSynonymValue();
        val.setName("trade");
        body.values().add(val);

        RMapping mapping = new RMapping();
        body.setMapping(mapping);

        List<? extends RNode> children = body.children();
        assertEquals(2, children.size());
        assertSame(val, children.get(0));
        assertSame(mapping, children.get(1));
    }

    @Test
    void rSynonymBody_childrenForMerge() {
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.MERGE);

        RMergeSynonymValue merge = new RMergeSynonymValue();
        merge.setName("trade");
        body.setMerge(merge);

        List<? extends RNode> children = body.children();
        assertEquals(1, children.size());
        assertSame(merge, children.get(0));
    }

    @Test
    void rSynonymBody_childrenForSetTo() {
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.SET_TO);

        RMappingSetTo setTo = new RMappingSetTo();
        body.setSetTo(setTo);

        List<? extends RNode> children = body.children();
        assertEquals(1, children.size());
        assertSame(setTo, children.get(0));
    }

    @Test
    void rSynonymBody_childrenEmptyForHint() {
        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.HINT);
        body.hints().add("something");
        // hints are strings, not RNode — children should be empty
        assertTrue(body.children().isEmpty());
    }

    // =========================================================================
    // RSynonymValue
    // =========================================================================

    @Test
    void rSynonymValue_defaultState() {
        RSynonymValue val = new RSynonymValue();
        assertNull(val.name());
        assertEquals(Optional.empty(), val.ref());
        assertEquals(Optional.empty(), val.refValue());
        assertEquals(Optional.empty(), val.path());
        assertEquals(Optional.empty(), val.maps());
    }

    @Test
    void rSynonymValue_setAllFields() {
        RSynonymValue val = new RSynonymValue();
        val.setName("trade");
        val.setRef(RSynonymRef.TAG);
        val.setRefValue(1);
        val.setPath("tradeHeader");
        val.setMaps(2);

        assertEquals("trade", val.name());
        assertEquals(Optional.of(RSynonymRef.TAG), val.ref());
        assertEquals(Optional.of(BigInteger.valueOf(1)), val.refValue());
        assertEquals(Optional.of("tradeHeader"), val.path());
        assertEquals(Optional.of(BigInteger.valueOf(2)), val.maps());
    }

    @Test
    void rSynonymValue_componentIdRef() {
        RSynonymValue val = new RSynonymValue();
        val.setName("component");
        val.setRef(RSynonymRef.COMPONENT_ID);
        val.setRefValue(42);

        assertEquals(Optional.of(RSynonymRef.COMPONENT_ID), val.ref());
        assertEquals(Optional.of(BigInteger.valueOf(42)), val.refValue());
    }

    @Test
    void rSynonymValue_isLeafNode() {
        RSynonymValue val = new RSynonymValue();
        val.setName("trade");
        assertTrue(val.children().isEmpty());
    }

    // =========================================================================
    // RMetaSynonymValue
    // =========================================================================

    @Test
    void rMetaSynonymValue_defaultState() {
        RMetaSynonymValue val = new RMetaSynonymValue();
        assertNull(val.name());
        assertEquals(Optional.empty(), val.ref());
        assertEquals(Optional.empty(), val.refValue());
        assertEquals(Optional.empty(), val.path());
        assertEquals(Optional.empty(), val.maps());
    }

    @Test
    void rMetaSynonymValue_setAllFields() {
        RMetaSynonymValue val = new RMetaSynonymValue();
        val.setName("scheme");
        val.setRef(RSynonymRef.TAG);
        val.setRefValue(5);
        val.setPath("header");
        val.setMaps(1);

        assertEquals("scheme", val.name());
        assertEquals(Optional.of(RSynonymRef.TAG), val.ref());
        assertEquals(Optional.of(BigInteger.valueOf(5)), val.refValue());
        assertEquals(Optional.of("header"), val.path());
        assertEquals(Optional.of(BigInteger.valueOf(1)), val.maps());
    }

    @Test
    void rMetaSynonymValue_isLeafNode() {
        RMetaSynonymValue val = new RMetaSynonymValue();
        val.setName("meta");
        assertTrue(val.children().isEmpty());
    }

    // =========================================================================
    // RClassSynonym
    // =========================================================================

    @Test
    void rClassSynonym_defaultState() {
        RClassSynonym cs = new RClassSynonym();
        assertTrue(cs.sources().isEmpty());
        assertEquals(Optional.empty(), cs.value());
        assertEquals(Optional.empty(), cs.meta());
        assertTrue(cs.children().isEmpty());
    }

    @Test
    void rClassSynonym_addSources() {
        RClassSynonym cs = new RClassSynonym();
        cs.sources().add("FpML");
        cs.sources().add("DTCC");

        assertEquals(2, cs.sources().size());
    }

    @Test
    void rClassSynonym_setValueAndMeta() {
        RClassSynonym cs = new RClassSynonym();
        cs.sources().add("FpML");

        RClassSynonymValue value = new RClassSynonymValue();
        value.setName("Trade");
        cs.setValue(value);

        RMetaSynonymValue meta = new RMetaSynonymValue();
        meta.setName("tradeHeader");
        cs.setMeta(meta);

        assertTrue(cs.value().isPresent());
        assertEquals("Trade", cs.value().get().name());
        assertTrue(cs.meta().isPresent());
        assertEquals("tradeHeader", cs.meta().get().name());
    }

    @Test
    void rClassSynonym_childrenIncludesValueAndMeta() {
        RClassSynonym cs = new RClassSynonym();

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
    void rClassSynonym_childrenWithOnlyValue() {
        RClassSynonym cs = new RClassSynonym();
        RClassSynonymValue value = new RClassSynonymValue();
        value.setName("Trade");
        cs.setValue(value);

        List<? extends RNode> children = cs.children();
        assertEquals(1, children.size());
        assertSame(value, children.get(0));
    }

    @Test
    void rClassSynonym_extendsRNode() {
        RClassSynonym cs = new RClassSynonym();
        assertInstanceOf(RNode.class, cs);
        assertFalse(RRootElement.class.isAssignableFrom(RClassSynonym.class));
    }

    // =========================================================================
    // RClassSynonymValue
    // =========================================================================

    @Test
    void rClassSynonymValue_defaultState() {
        RClassSynonymValue csv = new RClassSynonymValue();
        assertNull(csv.name());
        assertEquals(Optional.empty(), csv.ref());
        assertEquals(Optional.empty(), csv.refValue());
        assertEquals(Optional.empty(), csv.path());
    }

    @Test
    void rClassSynonymValue_setAllFields() {
        RClassSynonymValue csv = new RClassSynonymValue();
        csv.setName("Trade");
        csv.setRef(RSynonymRef.TAG);
        csv.setRefValue(10);
        csv.setPath("tradeHeader");

        assertEquals("Trade", csv.name());
        assertEquals(Optional.of(RSynonymRef.TAG), csv.ref());
        assertEquals(Optional.of(BigInteger.valueOf(10)), csv.refValue());
        assertEquals(Optional.of("tradeHeader"), csv.path());
    }

    @Test
    void rClassSynonymValue_isLeafNode() {
        RClassSynonymValue csv = new RClassSynonymValue();
        csv.setName("Test");
        assertTrue(csv.children().isEmpty());
    }

    // =========================================================================
    // RMergeSynonymValue
    // =========================================================================

    @Test
    void rMergeSynonymValue_defaultState() {
        RMergeSynonymValue msv = new RMergeSynonymValue();
        assertNull(msv.name());
        assertEquals(Optional.empty(), msv.excludePath());
    }

    @Test
    void rMergeSynonymValue_setAllFields() {
        RMergeSynonymValue msv = new RMergeSynonymValue();
        msv.setName("trade");
        msv.setExcludePath("excluded");

        assertEquals("trade", msv.name());
        assertEquals(Optional.of("excluded"), msv.excludePath());
    }

    @Test
    void rMergeSynonymValue_noExcludePath() {
        RMergeSynonymValue msv = new RMergeSynonymValue();
        msv.setName("trade");
        assertEquals(Optional.empty(), msv.excludePath());
    }

    @Test
    void rMergeSynonymValue_isLeafNode() {
        RMergeSynonymValue msv = new RMergeSynonymValue();
        msv.setName("trade");
        assertTrue(msv.children().isEmpty());
    }

    // =========================================================================
    // REnumSynonym
    // =========================================================================

    @Test
    void rEnumSynonym_defaultState() {
        REnumSynonym es = new REnumSynonym();
        assertTrue(es.sources().isEmpty());
        assertNull(es.value());
        assertEquals(Optional.empty(), es.definitionText());
        assertEquals(Optional.empty(), es.patternMatch());
        assertEquals(Optional.empty(), es.patternReplace());
        assertFalse(es.isRemoveHtml());
    }

    @Test
    void rEnumSynonym_setAllFields() {
        REnumSynonym es = new REnumSynonym();
        es.sources().add("FpML");
        es.setValue("ActiveTrade");
        es.setDefinitionText("An active trade");
        es.setPatternMatch("Active.*");
        es.setPatternReplace("Active");
        es.setRemoveHtml(true);

        assertEquals(1, es.sources().size());
        assertEquals("FpML", es.sources().get(0));
        assertEquals("ActiveTrade", es.value());
        assertEquals(Optional.of("An active trade"), es.definitionText());
        assertEquals(Optional.of("Active.*"), es.patternMatch());
        assertEquals(Optional.of("Active"), es.patternReplace());
        assertTrue(es.isRemoveHtml());
    }

    @Test
    void rEnumSynonym_multipleSources() {
        REnumSynonym es = new REnumSynonym();
        es.sources().add("FpML");
        es.sources().add("DTCC");
        es.sources().add("ISO20022");
        assertEquals(3, es.sources().size());
    }

    @Test
    void rEnumSynonym_minimalFields() {
        REnumSynonym es = new REnumSynonym();
        es.sources().add("FpML");
        es.setValue("Active");

        assertEquals("Active", es.value());
        assertEquals(Optional.empty(), es.definitionText());
        assertEquals(Optional.empty(), es.patternMatch());
        assertEquals(Optional.empty(), es.patternReplace());
        assertFalse(es.isRemoveHtml());
    }

    @Test
    void rEnumSynonym_isLeafNode() {
        REnumSynonym es = new REnumSynonym();
        es.sources().add("FpML");
        es.setValue("Active");
        assertTrue(es.children().isEmpty());
    }

    @Test
    void rEnumSynonym_extendsRNode() {
        REnumSynonym es = new REnumSynonym();
        assertInstanceOf(RNode.class, es);
        assertFalse(RRootElement.class.isAssignableFrom(REnumSynonym.class));
    }

    // =========================================================================
    // Placeholder type updates — verify typed lists compile and work
    // =========================================================================

    @Test
    void rDataType_classSynonymsTyped() {
        RDataType dt = new RDataType();
        dt.setName("Trade");

        RClassSynonym cs = new RClassSynonym();
        cs.sources().add("FpML");
        dt.classSynonyms().add(cs);

        assertEquals(1, dt.classSynonyms().size());
        assertInstanceOf(RClassSynonym.class, dt.classSynonyms().get(0));

        // Verify it appears in children
        assertTrue(dt.children().contains(cs));
    }

    @Test
    void rEnumeration_synonymsTyped() {
        REnumeration en = new REnumeration();
        en.setName("TradeStatus");

        RSynonym syn = new RSynonym();
        syn.sources().add("FpML");
        en.synonyms().add(syn);

        assertEquals(1, en.synonyms().size());
        assertInstanceOf(RSynonym.class, en.synonyms().get(0));
        assertTrue(en.children().contains(syn));
    }

    @Test
    void rChoice_classSynonymsTyped() {
        RChoice ch = new RChoice();
        ch.setName("ProductBase");

        RClassSynonym cs = new RClassSynonym();
        cs.sources().add("FpML");
        ch.classSynonyms().add(cs);

        assertEquals(1, ch.classSynonyms().size());
        assertInstanceOf(RClassSynonym.class, ch.classSynonyms().get(0));
        assertTrue(ch.children().contains(cs));
    }

    @Test
    void rAttribute_synonymsTyped() {
        RAttribute attr = new RAttribute();
        attr.setName("price");
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("number");
        attr.setTypeCall(tc);

        RSynonym syn = new RSynonym();
        syn.sources().add("FpML");
        attr.synonyms().add(syn);

        assertEquals(1, attr.synonyms().size());
        assertInstanceOf(RSynonym.class, attr.synonyms().get(0));
        assertTrue(attr.children().contains(syn));
    }

    @Test
    void rEnumValue_synonymsTyped() {
        REnumValue ev = new REnumValue();
        ev.setName("Active");

        REnumSynonym es = new REnumSynonym();
        es.sources().add("FpML");
        es.setValue("ActiveTrade");
        ev.synonyms().add(es);

        assertEquals(1, ev.synonyms().size());
        assertInstanceOf(REnumSynonym.class, ev.synonyms().get(0));
        assertTrue(ev.children().contains(es));
    }

    @Test
    void rChoiceOption_synonymsTyped() {
        RChoiceOption co = new RChoiceOption();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("Bond");
        co.setTypeCall(tc);

        RSynonym syn = new RSynonym();
        syn.sources().add("FpML");
        co.synonyms().add(syn);

        assertEquals(1, co.synonyms().size());
        assertInstanceOf(RSynonym.class, co.synonyms().get(0));
        assertTrue(co.children().contains(syn));
    }

    // =========================================================================
    // Integration: full synonym tree
    // =========================================================================

    @Test
    void integration_fullValueSynonym() {
        // [synonym FpML, DTCC value "trade" tag 1 path "header" maps 2
        //     meta scheme
        //     dateFormat "yyyy-MM-dd" pattern ".*Trade" "Trade" removeHtml]
        RSynonym syn = new RSynonym();
        syn.sources().add("FpML");
        syn.sources().add("DTCC");

        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.VALUE);

        RSynonymValue val = new RSynonymValue();
        val.setName("trade");
        val.setRef(RSynonymRef.TAG);
        val.setRefValue(1);
        val.setPath("header");
        val.setMaps(2);
        body.values().add(val);

        body.metaFields().add("scheme");
        body.setDateFormat("yyyy-MM-dd");
        body.setPatternMatch(".*Trade");
        body.setPatternReplace("Trade");
        body.setRemoveHtml(true);

        syn.setBody(body);

        // Verify tree structure
        assertEquals(2, syn.sources().size());
        assertEquals(SynonymBodyKind.VALUE, syn.body().kind());
        assertEquals(1, syn.body().values().size());
        assertEquals("trade", syn.body().values().get(0).name());
        assertEquals(Optional.of(RSynonymRef.TAG), syn.body().values().get(0).ref());
        assertEquals(Optional.of(BigInteger.valueOf(1)), syn.body().values().get(0).refValue());
        assertEquals(Optional.of("header"), syn.body().values().get(0).path());
        assertEquals(Optional.of(BigInteger.valueOf(2)), syn.body().values().get(0).maps());
        assertEquals(1, syn.body().metaFields().size());
        assertTrue(syn.body().isRemoveHtml());

        // children traversal
        List<? extends RNode> synChildren = syn.children();
        assertEquals(1, synChildren.size());
        assertSame(body, synChildren.get(0));

        List<? extends RNode> bodyChildren = body.children();
        assertEquals(1, bodyChildren.size()); // just the value (no mapping)
        assertSame(val, bodyChildren.get(0));
    }

    @Test
    void integration_classSynonymWithValueAndMeta() {
        // [synonym FpML value "Trade" tag 1 path "header"
        //     meta "scheme" componentId 5]
        RClassSynonym cs = new RClassSynonym();
        cs.sources().add("FpML");

        RClassSynonymValue csv = new RClassSynonymValue();
        csv.setName("Trade");
        csv.setRef(RSynonymRef.TAG);
        csv.setRefValue(1);
        csv.setPath("header");
        cs.setValue(csv);

        RMetaSynonymValue meta = new RMetaSynonymValue();
        meta.setName("scheme");
        meta.setRef(RSynonymRef.COMPONENT_ID);
        meta.setRefValue(5);
        cs.setMeta(meta);

        // Verify
        assertTrue(cs.value().isPresent());
        assertTrue(cs.meta().isPresent());
        assertEquals("Trade", cs.value().get().name());
        assertEquals(Optional.of(RSynonymRef.COMPONENT_ID), cs.meta().get().ref());

        List<? extends RNode> children = cs.children();
        assertEquals(2, children.size());
    }

    @Test
    void integration_enumSynonymOnEnumValue() {
        // Build an enum value with an enum synonym
        REnumValue ev = new REnumValue();
        ev.setName("Active");
        ev.setDisplayName("Active Trade");
        ev.setDefinition("An active trade status");

        REnumSynonym es = new REnumSynonym();
        es.sources().add("FpML");
        es.setValue("ActiveTrade");
        es.setDefinitionText("Maps to FpML active");
        es.setPatternMatch("Active.*");
        es.setPatternReplace("Active");
        ev.synonyms().add(es);

        assertEquals(1, ev.synonyms().size());
        assertEquals("ActiveTrade", ev.synonyms().get(0).value());
        assertEquals(Optional.of("Maps to FpML active"), ev.synonyms().get(0).definitionText());
    }

    @Test
    void integration_synonymWithMapping() {
        // [synonym FpML value "trade" [set when "path1" = "value1"]]
        RSynonym syn = new RSynonym();
        syn.sources().add("FpML");

        RSynonymBody body = new RSynonymBody();
        body.setKind(SynonymBodyKind.VALUE);

        RSynonymValue val = new RSynonymValue();
        val.setName("trade");
        body.values().add(val);

        RMapping mapping = new RMapping();
        RMappingInstance inst = new RMappingInstance();
        inst.setKind(MappingInstanceKind.SET_WHEN);
        mapping.instances().add(inst);
        body.setMapping(mapping);

        syn.setBody(body);

        assertTrue(syn.body().mapping().isPresent());
        assertEquals(1, syn.body().mapping().get().instances().size());

        // children should include both the value and the mapping
        List<? extends RNode> bodyChildren = body.children();
        assertEquals(2, bodyChildren.size());
        assertSame(val, bodyChildren.get(0));
        assertSame(mapping, bodyChildren.get(1));
    }

    // =========================================================================
    // Integration: end-to-end parse-and-build tests (Task 14)
    // =========================================================================

    @Nested
    class IntegrationParseAndBuild {

        // -- Synonym source top-level declaration ----------------------------

        @Test
        void parseSynonymSourceDeclaration() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "synonym source FpML");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RSynonymSource.class, model.rootElements().get(0));

            RSynonymSource src = (RSynonymSource) model.rootElements().get(0);
            assertEquals("FpML", src.name());
            assertSourceRangeSet(src);
            assertSame(model, src.parent());
        }

        // -- Attribute-level synonym (VALUE alternative) ---------------------

        @Test
        void parseAttributeSynonym_valueAlternative() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\" path \"p\"]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertEquals(1, attr.synonyms().size());

            RSynonym syn = attr.synonyms().get(0);
            assertEquals(List.of("FpML"), syn.sources());
            assertNotNull(syn.body());
            assertEquals(SynonymBodyKind.VALUE, syn.body().kind());
            assertSourceRangeSet(syn);
            assertSame(attr, syn.parent());

            // values
            assertEquals(1, syn.body().values().size());
            RSynonymValue sv = syn.body().values().get(0);
            assertEquals("barField", sv.name());
            assertEquals(Optional.of("p"), sv.path());
            assertEquals(Optional.empty(), sv.maps());
            assertSourceRangeSet(sv);
            assertSame(syn.body(), sv.parent());
        }

        @Test
        void parseAttributeSynonym_valueWithMapsAndTag() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML, DTCC value \"barField\" tag 42 path \"hdr\" maps 3]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);

            RSynonym syn = attr.synonyms().get(0);
            assertEquals(List.of("FpML", "DTCC"), syn.sources());

            RSynonymValue sv = syn.body().values().get(0);
            assertEquals("barField", sv.name());
            assertEquals(Optional.of(RSynonymRef.TAG), sv.ref());
            assertEquals(Optional.of(BigInteger.valueOf(42)), sv.refValue());
            assertEquals(Optional.of("hdr"), sv.path());
            assertEquals(Optional.of(BigInteger.valueOf(3)), sv.maps());
        }

        @Test
        void parseAttributeSynonym_valueWithTrailingMeta() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\" meta \"id\", \"href\"]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RSynonym syn = dt.attributes().get(0).synonyms().get(0);

            assertEquals(SynonymBodyKind.VALUE, syn.body().kind());
            assertEquals(1, syn.body().values().size());
            assertEquals(List.of("id", "href"), syn.body().metaFields());
        }

        // -- Attribute-level synonym (HINT alternative) ----------------------

        @Test
        void parseAttributeSynonym_hintAlternative() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML hint \"hint1\", \"hint2\"]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RSynonym syn = dt.attributes().get(0).synonyms().get(0);

            assertEquals(SynonymBodyKind.HINT, syn.body().kind());
            assertEquals(List.of("hint1", "hint2"), syn.body().hints());
        }

        // -- Attribute-level synonym (MERGE alternative) ---------------------

        @Test
        void parseAttributeSynonym_mergeAlternative() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML merge \"mergeField\" when path <> \"excluded\"]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RSynonym syn = dt.attributes().get(0).synonyms().get(0);

            assertEquals(SynonymBodyKind.MERGE, syn.body().kind());
            assertTrue(syn.body().merge().isPresent());
            RMergeSynonymValue merge = syn.body().merge().get();
            assertEquals("mergeField", merge.name());
            assertEquals(Optional.of("excluded"), merge.excludePath());
            assertSourceRangeSet(merge);
        }

        @Test
        void parseAttributeSynonym_mergeWithoutExcludePath() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML merge \"mergeField\"]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RSynonym syn = dt.attributes().get(0).synonyms().get(0);

            assertEquals(SynonymBodyKind.MERGE, syn.body().kind());
            RMergeSynonymValue merge = syn.body().merge().get();
            assertEquals("mergeField", merge.name());
            assertEquals(Optional.empty(), merge.excludePath());
        }

        // -- Attribute-level synonym (SET_TO alternative) --------------------

        @Test
        void parseAttributeSynonym_setToAlternative() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML set to \"value\" when \"p\" exists]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RSynonym syn = dt.attributes().get(0).synonyms().get(0);

            assertEquals(SynonymBodyKind.SET_TO, syn.body().kind());
            assertTrue(syn.body().setTo().isPresent());
            RMappingSetTo setTo = syn.body().setTo().get();
            assertEquals(1, setTo.instances().size());
            RMappingSetToInstance inst = setTo.instances().get(0);
            assertNotNull(inst.value());
            assertEquals(MapPrimaryKind.STRING, inst.value().kind());
            assertEquals(Optional.of("value"), inst.value().stringValue());
            assertTrue(inst.when().isPresent());
        }

        // -- Attribute-level synonym (META_ONLY alternative) -----------------

        @Test
        void parseAttributeSynonym_metaOnlyAlternative() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML meta \"id\", \"href\"]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RSynonym syn = dt.attributes().get(0).synonyms().get(0);

            assertEquals(SynonymBodyKind.META_ONLY, syn.body().kind());
            assertEquals(List.of("id", "href"), syn.body().metaFields());
        }

        // -- Synonym body trailing modifiers ---------------------------------

        @Test
        void parseAttributeSynonym_trailingModifiers() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " dateFormat \"yyyy-MM-dd\""
                    + " pattern \"Active.*\" \"Active\""
                    + " removeHtml"
                    + " mapper \"MyMapper\"]");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RSynonym syn = dt.attributes().get(0).synonyms().get(0);

            assertEquals(Optional.of("yyyy-MM-dd"), syn.body().dateFormat());
            assertEquals(Optional.of("Active.*"), syn.body().patternMatch());
            assertEquals(Optional.of("Active"), syn.body().patternReplace());
            assertTrue(syn.body().isRemoveHtml());
            assertEquals(Optional.of("MyMapper"), syn.body().mapper());
        }

        // -- Class-level synonym ---------------------------------------------

        @Test
        void parseClassSynonym_valueAndMeta() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    [synonym FpML value \"Foo\" meta \"id\"]\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertEquals(1, dt.classSynonyms().size());

            RClassSynonym cs = dt.classSynonyms().get(0);
            assertEquals(List.of("FpML"), cs.sources());
            assertTrue(cs.value().isPresent());
            assertEquals("Foo", cs.value().get().name());
            assertTrue(cs.meta().isPresent());
            assertEquals("id", cs.meta().get().name());
            assertSourceRangeSet(cs);
            assertSame(dt, cs.parent());
        }

        @Test
        void parseClassSynonym_valueOnly() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    [synonym FpML value \"Foo\" path \"header\"]\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RClassSynonym cs = dt.classSynonyms().get(0);

            assertTrue(cs.value().isPresent());
            RClassSynonymValue csv = cs.value().get();
            assertEquals("Foo", csv.name());
            assertEquals(Optional.of("header"), csv.path());
            assertFalse(cs.meta().isPresent());
        }

        // -- Enum-level synonym ----------------------------------------------

        @Test
        void parseEnumSynonym_minimal() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "enum Direction:\n"
                    + "    North\n"
                    + "        [synonym FpML value \"N\"]");
            REnumeration en = (REnumeration) model.rootElements().get(0);
            REnumValue val = en.values().get(0);
            assertEquals(1, val.synonyms().size());

            REnumSynonym es = val.synonyms().get(0);
            assertEquals(List.of("FpML"), es.sources());
            assertEquals("N", es.value());
            assertEquals(Optional.empty(), es.definitionText());
            assertEquals(Optional.empty(), es.patternMatch());
            assertFalse(es.isRemoveHtml());
            assertSourceRangeSet(es);
            assertSame(val, es.parent());
        }

        @Test
        void parseEnumSynonym_fullForm() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "enum Direction:\n"
                    + "    North\n"
                    + "        [synonym FpML value \"N\" definition \"North pole\""
                    + " pattern \"N.*\" \"N\" removeHtml]");
            REnumeration en = (REnumeration) model.rootElements().get(0);
            REnumSynonym es = en.values().get(0).synonyms().get(0);

            assertEquals("N", es.value());
            assertEquals(Optional.of("North pole"), es.definitionText());
            assertEquals(Optional.of("N.*"), es.patternMatch());
            assertEquals(Optional.of("N"), es.patternReplace());
            assertTrue(es.isRemoveHtml());
        }

        // -- Enumeration-level synonym (at the enum, not enum value) ---------

        @Test
        void parseEnumerationLevelSynonym() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "enum Direction:\n"
                    + "    [synonym FpML value \"DirSource\"]\n"
                    + "    North");
            REnumeration en = (REnumeration) model.rootElements().get(0);
            assertEquals(1, en.synonyms().size());
            RSynonym syn = en.synonyms().get(0);
            assertEquals(List.of("FpML"), syn.sources());
            assertEquals(SynonymBodyKind.VALUE, syn.body().kind());
            assertSame(en, syn.parent());
        }

        // -- Choice synonyms -------------------------------------------------

        @Test
        void parseChoiceClassSynonym() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "choice MyChoice:\n"
                    + "    [synonym FpML value \"ch\" meta \"id\"]\n"
                    + "    Foo\n"
                    + "    Bar");
            RChoice ch = (RChoice) model.rootElements().get(0);
            assertEquals(1, ch.classSynonyms().size());
            RClassSynonym cs = ch.classSynonyms().get(0);
            assertEquals("ch", cs.value().get().name());
            assertSame(ch, cs.parent());
        }

        @Test
        void parseChoiceOptionSynonym() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "choice MyChoice:\n"
                    + "    Foo\n"
                    + "        [synonym FpML value \"fooField\"]");
            RChoice ch = (RChoice) model.rootElements().get(0);
            RChoiceOption opt = ch.options().get(0);
            assertEquals(1, opt.synonyms().size());
            assertEquals("Foo", opt.typeCall().typeName());
            RSynonym syn = opt.synonyms().get(0);
            assertEquals("fooField", syn.body().values().get(0).name());
            assertSame(opt, syn.parent());
        }

        // -- Source range + parent pointer verification ----------------------

        @Test
        void synonymNodes_haveSourceRangesAndParents() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    [synonym FpML value \"Foo\" meta \"id\"]\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\"]");
            RDataType dt = (RDataType) model.rootElements().get(0);

            RClassSynonym cs = dt.classSynonyms().get(0);
            assertSourceRangeSet(cs);
            assertSame(dt, cs.parent());
            assertSourceRangeSet(cs.value().get());
            assertSame(cs, cs.value().get().parent());
            assertSourceRangeSet(cs.meta().get());
            assertSame(cs, cs.meta().get().parent());

            RSynonym syn = dt.attributes().get(0).synonyms().get(0);
            assertSourceRangeSet(syn);
            assertSourceRangeSet(syn.body());
            assertSame(syn, syn.body().parent());
            assertSourceRangeSet(syn.body().values().get(0));
            assertSame(syn.body(), syn.body().values().get(0).parent());
        }
    }
}
