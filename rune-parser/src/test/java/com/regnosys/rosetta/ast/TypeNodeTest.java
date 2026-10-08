package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RParameter;
import com.regnosys.rosetta.ast.supporting.RRecordFeature;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgument;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgumentExpression;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RLibraryFunction;
import com.regnosys.rosetta.ast.types.RMetaType;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for type declaration nodes and their supporting child nodes (Task 3).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, list fields, and the {@code children()} traversal.
 */
class TypeNodeTest extends BaseAstTest {

    // =========================================================================
    // RDataType
    // =========================================================================

    @Test
    void rDataType_defaultState() {
        RDataType dt = new RDataType();
        assertNull(dt.name());
        assertEquals(Optional.empty(), dt.superTypeName());
        assertEquals(Optional.empty(), dt.definition());
        assertTrue(dt.attributes().isEmpty());
        assertTrue(dt.docReferences().isEmpty());
        assertTrue(dt.annotationRefs().isEmpty());
        assertTrue(dt.classSynonyms().isEmpty());
        assertTrue(dt.conditions().isEmpty());
        assertTrue(dt.children().isEmpty());
    }

    @Test
    void rDataType_setNameAndSuperType() {
        RDataType dt = new RDataType();
        dt.setName("TradeState");
        dt.setSuperTypeName("com.example.BaseType");

        assertEquals("TradeState", dt.name());
        assertEquals(Optional.of("com.example.BaseType"), dt.superTypeName());
    }

    @Test
    void rDataType_setDefinition() {
        RDataType dt = new RDataType();
        dt.setName("TradeState");
        dt.setDefinition("Defines the state of a trade.");

        assertEquals(Optional.of("Defines the state of a trade."), dt.definition());
    }

    @Test
    void rDataType_implementsRDefinable() {
        RDataType dt = new RDataType();
        assertInstanceOf(RDefinable.class, dt);
    }

    @Test
    void rDataType_extendsRRootElement() {
        RDataType dt = new RDataType();
        assertInstanceOf(RRootElement.class, dt);
        assertInstanceOf(RNode.class, dt);
    }

    @Test
    void rDataType_addAttributes() {
        RDataType dt = new RDataType();
        dt.setName("TradeState");

        RAttribute attr1 = new RAttribute();
        attr1.setName("trade");
        RTypeCall tc1 = new RTypeCall();
        tc1.setTypeName("Trade");
        attr1.setTypeCall(tc1);

        RAttribute attr2 = new RAttribute();
        attr2.setName("state");
        RTypeCall tc2 = new RTypeCall();
        tc2.setTypeName("State");
        attr2.setTypeCall(tc2);

        dt.attributes().add(attr1);
        dt.attributes().add(attr2);

        assertEquals(2, dt.attributes().size());
        assertEquals("trade", dt.attributes().get(0).name());
        assertEquals("state", dt.attributes().get(1).name());
    }

    @Test
    void rDataType_childrenIncludesAttributes() {
        RDataType dt = new RDataType();

        RAttribute attr = new RAttribute();
        attr.setName("price");
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("number");
        attr.setTypeCall(tc);

        dt.attributes().add(attr);

        List<? extends RNode> children = dt.children();
        assertEquals(1, children.size());
        assertSame(attr, children.get(0));
    }

    @Test
    void rDataType_noSuperTypeName() {
        RDataType dt = new RDataType();
        dt.setName("Standalone");
        assertEquals(Optional.empty(), dt.superTypeName());
    }

    // =========================================================================
    // REnumeration
    // =========================================================================

    @Test
    void rEnumeration_defaultState() {
        REnumeration en = new REnumeration();
        assertNull(en.name());
        assertEquals(Optional.empty(), en.superTypeName());
        assertEquals(Optional.empty(), en.definition());
        assertTrue(en.values().isEmpty());
        assertTrue(en.docReferences().isEmpty());
        assertTrue(en.annotationRefs().isEmpty());
        assertTrue(en.synonyms().isEmpty());
        assertTrue(en.children().isEmpty());
    }

    @Test
    void rEnumeration_setNameAndSuperType() {
        REnumeration en = new REnumeration();
        en.setName("CurrencyCode");
        en.setSuperTypeName("com.example.BaseEnum");

        assertEquals("CurrencyCode", en.name());
        assertEquals(Optional.of("com.example.BaseEnum"), en.superTypeName());
    }

    @Test
    void rEnumeration_setDefinition() {
        REnumeration en = new REnumeration();
        en.setDefinition("ISO 4217 currency codes.");

        assertEquals(Optional.of("ISO 4217 currency codes."), en.definition());
    }

    @Test
    void rEnumeration_implementsRDefinable() {
        REnumeration en = new REnumeration();
        assertInstanceOf(RDefinable.class, en);
    }

    @Test
    void rEnumeration_extendsRRootElement() {
        REnumeration en = new REnumeration();
        assertInstanceOf(RRootElement.class, en);
    }

    @Test
    void rEnumeration_addValues() {
        REnumeration en = new REnumeration();
        en.setName("DayOfWeek");

        REnumValue val1 = new REnumValue();
        val1.setName("Monday");
        REnumValue val2 = new REnumValue();
        val2.setName("Tuesday");

        en.values().add(val1);
        en.values().add(val2);

        assertEquals(2, en.values().size());
        assertEquals("Monday", en.values().get(0).name());
        assertEquals("Tuesday", en.values().get(1).name());
    }

    @Test
    void rEnumeration_childrenIncludesValues() {
        REnumeration en = new REnumeration();

        REnumValue val = new REnumValue();
        val.setName("Active");
        en.values().add(val);

        List<? extends RNode> children = en.children();
        assertEquals(1, children.size());
        assertSame(val, children.get(0));
    }

    // =========================================================================
    // RChoice
    // =========================================================================

    @Test
    void rChoice_defaultState() {
        RChoice ch = new RChoice();
        assertNull(ch.name());
        assertEquals(Optional.empty(), ch.definition());
        assertTrue(ch.options().isEmpty());
        assertTrue(ch.annotationRefs().isEmpty());
        assertTrue(ch.classSynonyms().isEmpty());
        assertTrue(ch.children().isEmpty());
    }

    @Test
    void rChoice_setNameAndDefinition() {
        RChoice ch = new RChoice();
        ch.setName("PayoutBase");
        ch.setDefinition("A choice between payout types.");

        assertEquals("PayoutBase", ch.name());
        assertEquals(Optional.of("A choice between payout types."), ch.definition());
    }

    @Test
    void rChoice_noSuperTypeName() {
        // RChoice has no superTypeName — grammar does not support extends for choice
        RChoice ch = new RChoice();
        ch.setName("PayoutBase");
        // No setSuperTypeName method should exist — verified by structure
        assertInstanceOf(RDefinable.class, ch);
        assertInstanceOf(RRootElement.class, ch);
    }

    @Test
    void rChoice_addOptions() {
        RChoice ch = new RChoice();
        ch.setName("PayoutBase");

        RChoiceOption opt1 = new RChoiceOption();
        RTypeCall tc1 = new RTypeCall();
        tc1.setTypeName("CashPayout");
        opt1.setTypeCall(tc1);

        RChoiceOption opt2 = new RChoiceOption();
        RTypeCall tc2 = new RTypeCall();
        tc2.setTypeName("PhysicalPayout");
        opt2.setTypeCall(tc2);

        ch.options().add(opt1);
        ch.options().add(opt2);

        assertEquals(2, ch.options().size());
        assertEquals("CashPayout", ch.options().get(0).typeCall().typeName());
        assertEquals("PhysicalPayout", ch.options().get(1).typeCall().typeName());
    }

    @Test
    void rChoice_childrenIncludesOptions() {
        RChoice ch = new RChoice();

        RChoiceOption opt = new RChoiceOption();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("CashPayout");
        opt.setTypeCall(tc);
        ch.options().add(opt);

        List<? extends RNode> children = ch.children();
        assertEquals(1, children.size());
        assertSame(opt, children.get(0));
    }

    // =========================================================================
    // RChoiceOption
    // =========================================================================

    @Test
    void rChoiceOption_defaultState() {
        RChoiceOption co = new RChoiceOption();
        assertNull(co.typeCall());
        assertEquals(Optional.empty(), co.definition());
        assertTrue(co.docReferences().isEmpty());
        assertTrue(co.annotationRefs().isEmpty());
        assertTrue(co.synonyms().isEmpty());
        assertTrue(co.labelAnnotations().isEmpty());
        assertTrue(co.ruleReferenceAnnotations().isEmpty());
        assertTrue(co.children().isEmpty());
    }

    @Test
    void rChoiceOption_setTypeCallAndDefinition() {
        RChoiceOption co = new RChoiceOption();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("InterestRatePayout");
        co.setTypeCall(tc);
        co.setDefinition("An interest rate payout option.");

        assertEquals("InterestRatePayout", co.typeCall().typeName());
        assertEquals(Optional.of("An interest rate payout option."), co.definition());
    }

    @Test
    void rChoiceOption_implementsRDefinable() {
        RChoiceOption co = new RChoiceOption();
        assertInstanceOf(RDefinable.class, co);
    }

    @Test
    void rChoiceOption_childrenIncludesTypeCall() {
        RChoiceOption co = new RChoiceOption();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("CreditDefaultPayout");
        co.setTypeCall(tc);

        List<? extends RNode> children = co.children();
        assertEquals(1, children.size());
        assertSame(tc, children.get(0));
    }

    @Test
    void rChoiceOption_noNameField() {
        // RChoiceOption has no separate name — the typeCall IS the identity
        RChoiceOption co = new RChoiceOption();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("OptionPayout");
        co.setTypeCall(tc);

        // Identity comes from typeCall, not a name field
        assertEquals("OptionPayout", co.typeCall().typeName());
    }

    // =========================================================================
    // RAttribute
    // =========================================================================

    @Test
    void rAttribute_defaultState() {
        RAttribute attr = new RAttribute();
        assertFalse(attr.isOverride());
        assertNull(attr.name());
        assertNull(attr.typeCall());
        assertEquals(Optional.empty(), attr.cardinality());
        assertEquals(Optional.empty(), attr.definition());
        assertTrue(attr.docReferences().isEmpty());
        assertTrue(attr.annotationRefs().isEmpty());
        assertTrue(attr.synonyms().isEmpty());
        assertTrue(attr.labelAnnotations().isEmpty());
        assertTrue(attr.ruleReferenceAnnotations().isEmpty());
        assertTrue(attr.children().isEmpty());
    }

    @Test
    void rAttribute_setAllFields() {
        RAttribute attr = new RAttribute();
        attr.setOverride(true);
        attr.setName("notionalAmount");

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("Money");
        attr.setTypeCall(tc);

        RCardinality card = new RCardinality();
        card.setInf(1);
        card.setSup(1);
        card.setUnbounded(false);
        attr.setCardinality(card);

        attr.setDefinition("The notional amount of the trade.");

        assertTrue(attr.isOverride());
        assertEquals("notionalAmount", attr.name());
        assertEquals("Money", attr.typeCall().typeName());
        assertTrue(attr.cardinality().isPresent());
        assertEquals(BigInteger.ONE, attr.cardinality().get().inf());
        assertEquals(BigInteger.ONE, attr.cardinality().get().sup());
        assertEquals(Optional.of("The notional amount of the trade."), attr.definition());
    }

    @Test
    void rAttribute_implementsRDefinable() {
        RAttribute attr = new RAttribute();
        assertInstanceOf(RDefinable.class, attr);
    }

    @Test
    void rAttribute_extendsRNode() {
        RAttribute attr = new RAttribute();
        assertInstanceOf(RNode.class, attr);
        // RAttribute is a supporting node, not a root element
        assertFalse(RRootElement.class.isAssignableFrom(RAttribute.class));
    }

    @Test
    void rAttribute_childrenIncludesTypeCallAndCardinality() {
        RAttribute attr = new RAttribute();

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("string");
        attr.setTypeCall(tc);

        RCardinality card = new RCardinality();
        card.setInf(0);
        card.setSup(1);
        attr.setCardinality(card);

        List<? extends RNode> children = attr.children();
        assertEquals(2, children.size());
        assertSame(tc, children.get(0));
        assertSame(card, children.get(1));
    }

    @Test
    void rAttribute_childrenWithoutCardinality() {
        RAttribute attr = new RAttribute();

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("int");
        attr.setTypeCall(tc);

        List<? extends RNode> children = attr.children();
        assertEquals(1, children.size());
        assertSame(tc, children.get(0));
    }

    @Test
    void rAttribute_overrideDefaultFalse() {
        RAttribute attr = new RAttribute();
        assertFalse(attr.isOverride());
    }

    @Test
    void rAttribute_cardinalityOptional() {
        RAttribute attr = new RAttribute();
        assertEquals(Optional.empty(), attr.cardinality());

        RCardinality card = new RCardinality();
        card.setInf(0);
        card.setSup(RCardinality.UNBOUNDED);
        card.setUnbounded(true);
        attr.setCardinality(card);

        assertTrue(attr.cardinality().isPresent());
        assertTrue(attr.cardinality().get().isUnbounded());
    }

    // =========================================================================
    // REnumValue
    // =========================================================================

    @Test
    void rEnumValue_defaultState() {
        REnumValue ev = new REnumValue();
        assertNull(ev.name());
        assertEquals(Optional.empty(), ev.displayName());
        assertEquals(Optional.empty(), ev.definition());
        assertTrue(ev.docReferences().isEmpty());
        assertTrue(ev.annotationRefs().isEmpty());
        assertTrue(ev.synonyms().isEmpty());
        assertTrue(ev.children().isEmpty());
    }

    @Test
    void rEnumValue_setAllFields() {
        REnumValue ev = new REnumValue();
        ev.setName("Active");
        ev.setDisplayName("Active Status");
        ev.setDefinition("Indicates an active state.");

        assertEquals("Active", ev.name());
        assertEquals(Optional.of("Active Status"), ev.displayName());
        assertEquals(Optional.of("Indicates an active state."), ev.definition());
    }

    @Test
    void rEnumValue_implementsRDefinable() {
        REnumValue ev = new REnumValue();
        assertInstanceOf(RDefinable.class, ev);
    }

    @Test
    void rEnumValue_noDisplayName() {
        REnumValue ev = new REnumValue();
        ev.setName("Terminated");
        assertEquals(Optional.empty(), ev.displayName());
    }

    @Test
    void rEnumValue_isLeafByDefault() {
        // Without any doc references, annotations, or synonyms, it's a leaf
        REnumValue ev = new REnumValue();
        ev.setName("Pending");
        assertTrue(ev.children().isEmpty());
    }

    // =========================================================================
    // RTypeAlias
    // =========================================================================

    @Test
    void rTypeAlias_defaultState() {
        RTypeAlias ta = new RTypeAlias();
        assertNull(ta.name());
        assertNull(ta.typeCall());
        assertEquals(Optional.empty(), ta.definition());
        assertTrue(ta.typeParameters().isEmpty());
        assertTrue(ta.conditions().isEmpty());
        assertTrue(ta.children().isEmpty());
    }

    @Test
    void rTypeAlias_setNameAndTypeCall() {
        RTypeAlias ta = new RTypeAlias();
        ta.setName("PositiveNumber");

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("number");
        ta.setTypeCall(tc);

        assertEquals("PositiveNumber", ta.name());
        assertEquals("number", ta.typeCall().typeName());
    }

    @Test
    void rTypeAlias_setDefinition() {
        RTypeAlias ta = new RTypeAlias();
        ta.setDefinition("A positive number type alias.");

        assertEquals(Optional.of("A positive number type alias."), ta.definition());
    }

    @Test
    void rTypeAlias_implementsRDefinable() {
        RTypeAlias ta = new RTypeAlias();
        assertInstanceOf(RDefinable.class, ta);
    }

    @Test
    void rTypeAlias_extendsRRootElement() {
        RTypeAlias ta = new RTypeAlias();
        assertInstanceOf(RRootElement.class, ta);
    }

    @Test
    void rTypeAlias_addTypeParameters() {
        RTypeAlias ta = new RTypeAlias();
        ta.setName("Constrained");

        RTypeParameter tp1 = new RTypeParameter();
        tp1.setName("min");
        RTypeCall tpTc1 = new RTypeCall();
        tpTc1.setTypeName("int");
        tp1.setTypeCall(tpTc1);

        RTypeParameter tp2 = new RTypeParameter();
        tp2.setName("max");
        RTypeCall tpTc2 = new RTypeCall();
        tpTc2.setTypeName("int");
        tp2.setTypeCall(tpTc2);

        ta.typeParameters().add(tp1);
        ta.typeParameters().add(tp2);

        assertEquals(2, ta.typeParameters().size());
        assertEquals("min", ta.typeParameters().get(0).name());
        assertEquals("max", ta.typeParameters().get(1).name());
    }

    @Test
    void rTypeAlias_childrenIncludesTypeParamsAndTypeCall() {
        RTypeAlias ta = new RTypeAlias();

        RTypeParameter tp = new RTypeParameter();
        tp.setName("digits");
        ta.typeParameters().add(tp);

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("number");
        ta.setTypeCall(tc);

        List<? extends RNode> children = ta.children();
        assertEquals(2, children.size());
        assertSame(tp, children.get(0));
        assertSame(tc, children.get(1));
    }

    @Test
    void rTypeAlias_childrenWithoutTypeCall() {
        RTypeAlias ta = new RTypeAlias();

        RTypeParameter tp = new RTypeParameter();
        tp.setName("digits");
        ta.typeParameters().add(tp);

        List<? extends RNode> children = ta.children();
        assertEquals(1, children.size());
        assertSame(tp, children.get(0));
    }

    // =========================================================================
    // RBasicType
    // =========================================================================

    @Test
    void rBasicType_defaultState() {
        RBasicType bt = new RBasicType();
        assertNull(bt.name());
        assertEquals(Optional.empty(), bt.definition());
        assertTrue(bt.typeParameters().isEmpty());
        assertTrue(bt.children().isEmpty());
    }

    @Test
    void rBasicType_setNameAndDefinition() {
        RBasicType bt = new RBasicType();
        bt.setName("number");
        bt.setDefinition("A numeric value.");

        assertEquals("number", bt.name());
        assertEquals(Optional.of("A numeric value."), bt.definition());
    }

    @Test
    void rBasicType_implementsRDefinable() {
        RBasicType bt = new RBasicType();
        assertInstanceOf(RDefinable.class, bt);
    }

    @Test
    void rBasicType_extendsRRootElement() {
        RBasicType bt = new RBasicType();
        assertInstanceOf(RRootElement.class, bt);
    }

    @Test
    void rBasicType_addTypeParameters() {
        RBasicType bt = new RBasicType();
        bt.setName("number");

        RTypeParameter tp1 = new RTypeParameter();
        tp1.setName("digits");
        RTypeCall tpTc1 = new RTypeCall();
        tpTc1.setTypeName("int");
        tp1.setTypeCall(tpTc1);

        RTypeParameter tp2 = new RTypeParameter();
        tp2.setName("fractionalDigits");
        RTypeCall tpTc2 = new RTypeCall();
        tpTc2.setTypeName("int");
        tp2.setTypeCall(tpTc2);

        bt.typeParameters().add(tp1);
        bt.typeParameters().add(tp2);

        assertEquals(2, bt.typeParameters().size());
        assertEquals("digits", bt.typeParameters().get(0).name());
        assertEquals("fractionalDigits", bt.typeParameters().get(1).name());
    }

    @Test
    void rBasicType_childrenIncludesTypeParameters() {
        RBasicType bt = new RBasicType();

        RTypeParameter tp = new RTypeParameter();
        tp.setName("digits");
        bt.typeParameters().add(tp);

        List<? extends RNode> children = bt.children();
        assertEquals(1, children.size());
        assertSame(tp, children.get(0));
    }

    // =========================================================================
    // RRecordType
    // =========================================================================

    @Test
    void rRecordType_defaultState() {
        RRecordType rt = new RRecordType();
        assertNull(rt.name());
        assertEquals(Optional.empty(), rt.definition());
        assertTrue(rt.features().isEmpty());
        assertTrue(rt.children().isEmpty());
    }

    @Test
    void rRecordType_setNameAndDefinition() {
        RRecordType rt = new RRecordType();
        rt.setName("date");
        rt.setDefinition("A calendar date.");

        assertEquals("date", rt.name());
        assertEquals(Optional.of("A calendar date."), rt.definition());
    }

    @Test
    void rRecordType_implementsRDefinable() {
        RRecordType rt = new RRecordType();
        assertInstanceOf(RDefinable.class, rt);
    }

    @Test
    void rRecordType_extendsRRootElement() {
        RRecordType rt = new RRecordType();
        assertInstanceOf(RRootElement.class, rt);
    }

    @Test
    void rRecordType_addFeatures() {
        RRecordType rt = new RRecordType();
        rt.setName("date");

        RRecordFeature f1 = new RRecordFeature();
        f1.setName("year");
        RTypeCall tc1 = new RTypeCall();
        tc1.setTypeName("int");
        f1.setTypeCall(tc1);

        RRecordFeature f2 = new RRecordFeature();
        f2.setName("month");
        RTypeCall tc2 = new RTypeCall();
        tc2.setTypeName("int");
        f2.setTypeCall(tc2);

        RRecordFeature f3 = new RRecordFeature();
        f3.setName("day");
        RTypeCall tc3 = new RTypeCall();
        tc3.setTypeName("int");
        f3.setTypeCall(tc3);

        rt.features().add(f1);
        rt.features().add(f2);
        rt.features().add(f3);

        assertEquals(3, rt.features().size());
        assertEquals("year", rt.features().get(0).name());
        assertEquals("month", rt.features().get(1).name());
        assertEquals("day", rt.features().get(2).name());
    }

    @Test
    void rRecordType_childrenIncludesFeatures() {
        RRecordType rt = new RRecordType();

        RRecordFeature f = new RRecordFeature();
        f.setName("year");
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("int");
        f.setTypeCall(tc);
        rt.features().add(f);

        List<? extends RNode> children = rt.children();
        assertEquals(1, children.size());
        assertSame(f, children.get(0));
    }

    // =========================================================================
    // RMetaType
    // =========================================================================

    @Test
    void rMetaType_defaultState() {
        RMetaType mt = new RMetaType();
        assertNull(mt.name());
        assertNull(mt.typeCall());
        assertTrue(mt.children().isEmpty());
    }

    @Test
    void rMetaType_setNameAndTypeCall() {
        RMetaType mt = new RMetaType();
        mt.setName("reference");

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("string");
        mt.setTypeCall(tc);

        assertEquals("reference", mt.name());
        assertEquals("string", mt.typeCall().typeName());
    }

    @Test
    void rMetaType_doesNotImplementRDefinable() {
        // Meta types do NOT have definitions — grammar has no definable?
        RMetaType mt = new RMetaType();
        assertFalse(mt instanceof RDefinable);
    }

    @Test
    void rMetaType_extendsRRootElement() {
        RMetaType mt = new RMetaType();
        assertInstanceOf(RRootElement.class, mt);
    }

    @Test
    void rMetaType_childrenIncludesTypeCall() {
        RMetaType mt = new RMetaType();

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("string");
        mt.setTypeCall(tc);

        List<? extends RNode> children = mt.children();
        assertEquals(1, children.size());
        assertSame(tc, children.get(0));
    }

    @Test
    void rMetaType_childrenEmptyWhenNoTypeCall() {
        RMetaType mt = new RMetaType();
        assertTrue(mt.children().isEmpty());
    }

    // =========================================================================
    // RLibraryFunction
    // =========================================================================

    @Test
    void rLibraryFunction_defaultState() {
        RLibraryFunction lf = new RLibraryFunction();
        assertNull(lf.name());
        assertNull(lf.returnType());
        assertEquals(Optional.empty(), lf.definition());
        assertTrue(lf.parameters().isEmpty());
        assertTrue(lf.children().isEmpty());
    }

    @Test
    void rLibraryFunction_setNameAndReturnType() {
        RLibraryFunction lf = new RLibraryFunction();
        lf.setName("Max");

        RTypeCall rt = new RTypeCall();
        rt.setTypeName("number");
        lf.setReturnType(rt);

        assertEquals("Max", lf.name());
        assertEquals("number", lf.returnType().typeName());
    }

    @Test
    void rLibraryFunction_setDefinition() {
        RLibraryFunction lf = new RLibraryFunction();
        lf.setDefinition("Returns the maximum value.");

        assertEquals(Optional.of("Returns the maximum value."), lf.definition());
    }

    @Test
    void rLibraryFunction_implementsRDefinable() {
        RLibraryFunction lf = new RLibraryFunction();
        assertInstanceOf(RDefinable.class, lf);
    }

    @Test
    void rLibraryFunction_extendsRRootElement() {
        RLibraryFunction lf = new RLibraryFunction();
        assertInstanceOf(RRootElement.class, lf);
    }

    @Test
    void rLibraryFunction_addParameters() {
        RLibraryFunction lf = new RLibraryFunction();
        lf.setName("Max");

        RParameter p1 = new RParameter();
        p1.setName("a");
        RTypeCall ptc1 = new RTypeCall();
        ptc1.setTypeName("number");
        p1.setTypeCall(ptc1);

        RParameter p2 = new RParameter();
        p2.setName("b");
        RTypeCall ptc2 = new RTypeCall();
        ptc2.setTypeName("number");
        p2.setTypeCall(ptc2);

        lf.parameters().add(p1);
        lf.parameters().add(p2);

        assertEquals(2, lf.parameters().size());
        assertEquals("a", lf.parameters().get(0).name());
        assertEquals("b", lf.parameters().get(1).name());
    }

    @Test
    void rLibraryFunction_childrenIncludesParametersAndReturnType() {
        RLibraryFunction lf = new RLibraryFunction();

        RParameter p = new RParameter();
        p.setName("input");
        RTypeCall ptc = new RTypeCall();
        ptc.setTypeName("number");
        p.setTypeCall(ptc);
        lf.parameters().add(p);

        RTypeCall rt = new RTypeCall();
        rt.setTypeName("number");
        lf.setReturnType(rt);

        List<? extends RNode> children = lf.children();
        assertEquals(2, children.size());
        assertSame(p, children.get(0));
        assertSame(rt, children.get(1));
    }

    @Test
    void rLibraryFunction_childrenWithoutReturnType() {
        RLibraryFunction lf = new RLibraryFunction();

        RParameter p = new RParameter();
        p.setName("input");
        lf.parameters().add(p);

        List<? extends RNode> children = lf.children();
        assertEquals(1, children.size());
        assertSame(p, children.get(0));
    }

    // =========================================================================
    // Integration: Full data type tree
    // =========================================================================

    @Test
    void fullDataType_deepChildrenTraversal() {
        // Build: type TradeState extends BaseType: <"A trade state">
        //     override trade Trade (1..1) <"The trade">
        //     state State (0..1) <"The state">
        RDataType dt = new RDataType();
        dt.setName("TradeState");
        dt.setSuperTypeName("BaseType");
        dt.setDefinition("A trade state.");

        RAttribute attr1 = new RAttribute();
        attr1.setOverride(true);
        attr1.setName("trade");
        RTypeCall tc1 = new RTypeCall();
        tc1.setTypeName("Trade");
        attr1.setTypeCall(tc1);
        RCardinality card1 = new RCardinality();
        card1.setInf(1);
        card1.setSup(1);
        attr1.setCardinality(card1);
        attr1.setDefinition("The trade.");

        RAttribute attr2 = new RAttribute();
        attr2.setName("state");
        RTypeCall tc2 = new RTypeCall();
        tc2.setTypeName("State");
        attr2.setTypeCall(tc2);
        RCardinality card2 = new RCardinality();
        card2.setInf(0);
        card2.setSup(1);
        attr2.setCardinality(card2);
        attr2.setDefinition("The state.");

        dt.attributes().add(attr1);
        dt.attributes().add(attr2);

        // Top-level children: 2 attributes
        List<? extends RNode> dtChildren = dt.children();
        assertEquals(2, dtChildren.size());
        assertSame(attr1, dtChildren.get(0));
        assertSame(attr2, dtChildren.get(1));

        // Each attribute's children: typeCall + cardinality
        List<? extends RNode> attr1Children = attr1.children();
        assertEquals(2, attr1Children.size());
        assertSame(tc1, attr1Children.get(0));
        assertSame(card1, attr1Children.get(1));

        // Verify attribute properties
        assertTrue(attr1.isOverride());
        assertFalse(attr2.isOverride());
        assertEquals(Optional.of("The trade."), attr1.definition());
    }

    @Test
    void fullEnumeration_deepChildrenTraversal() {
        // Build: enum DayOfWeek extends BaseDays: <"Days of the week">
        //     Monday displayName "Mon" <"First day">
        //     Tuesday <"Second day">
        REnumeration en = new REnumeration();
        en.setName("DayOfWeek");
        en.setSuperTypeName("BaseDays");
        en.setDefinition("Days of the week.");

        REnumValue val1 = new REnumValue();
        val1.setName("Monday");
        val1.setDisplayName("Mon");
        val1.setDefinition("First day.");

        REnumValue val2 = new REnumValue();
        val2.setName("Tuesday");
        val2.setDefinition("Second day.");

        en.values().add(val1);
        en.values().add(val2);

        List<? extends RNode> enChildren = en.children();
        assertEquals(2, enChildren.size());
        assertSame(val1, enChildren.get(0));
        assertSame(val2, enChildren.get(1));

        assertEquals(Optional.of("Mon"), val1.displayName());
        assertEquals(Optional.empty(), val2.displayName());
    }

    @Test
    void fullChoice_deepChildrenTraversal() {
        // Build: choice PayoutBase: <"A payout choice">
        //     CashPayout <"Cash payout option">
        //     PhysicalPayout
        RChoice ch = new RChoice();
        ch.setName("PayoutBase");
        ch.setDefinition("A payout choice.");

        RChoiceOption opt1 = new RChoiceOption();
        RTypeCall tc1 = new RTypeCall();
        tc1.setTypeName("CashPayout");
        opt1.setTypeCall(tc1);
        opt1.setDefinition("Cash payout option.");

        RChoiceOption opt2 = new RChoiceOption();
        RTypeCall tc2 = new RTypeCall();
        tc2.setTypeName("PhysicalPayout");
        opt2.setTypeCall(tc2);

        ch.options().add(opt1);
        ch.options().add(opt2);

        List<? extends RNode> chChildren = ch.children();
        assertEquals(2, chChildren.size());

        // Each option's children include its typeCall
        List<? extends RNode> opt1Children = opt1.children();
        assertEquals(1, opt1Children.size());
        assertSame(tc1, opt1Children.get(0));

        assertEquals(Optional.of("Cash payout option."), opt1.definition());
        assertEquals(Optional.empty(), opt2.definition());
    }

    // =========================================================================
    // Integration tests (parse + build) — Task 11
    // =========================================================================

    @Nested
    class IntegrationParseAndBuild {

        // -- DataType tests --------------------------------------------------

        @Test
        void parseDataTypeMinimal() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RDataType.class, model.rootElements().get(0));

            RDataType dt = (RDataType) model.rootElements().get(0);
            assertEquals("Foo", dt.name());
            assertEquals(Optional.empty(), dt.superTypeName());
            assertEquals(Optional.empty(), dt.definition());
            assertEquals(1, dt.attributes().size());

            RAttribute attr = dt.attributes().get(0);
            assertEquals("bar", attr.name());
            assertEquals("string", attr.typeCall().typeName());
            assertTrue(attr.cardinality().isPresent());
            assertEquals(BigInteger.ONE, attr.cardinality().get().inf());
            assertEquals(BigInteger.ONE, attr.cardinality().get().sup());
            assertFalse(attr.cardinality().get().isUnbounded());
            assertFalse(attr.isOverride());
        }

        @Test
        void parseDataTypeWithSuperTypeAndDefinition() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo extends Bar: <\"A foo.\">\n"
                    + "    baz int (0..*)");
            RDataType dt = (RDataType) model.rootElements().get(0);

            assertEquals("Foo", dt.name());
            assertEquals(Optional.of("Bar"), dt.superTypeName());
            assertEquals(Optional.of("A foo."), dt.definition());
            assertEquals(1, dt.attributes().size());

            RAttribute attr = dt.attributes().get(0);
            assertEquals("baz", attr.name());
            assertEquals("int", attr.typeCall().typeName());
            assertTrue(attr.cardinality().isPresent());
            assertEquals(BigInteger.ZERO, attr.cardinality().get().inf());
            assertTrue(attr.cardinality().get().isUnbounded());
            assertEquals(RCardinality.UNBOUNDED, attr.cardinality().get().sup());
        }

        @Test
        void parseDataTypeWithoutSuperType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Standalone: <\"No parent.\">\n"
                    + "    name string (0..1) <\"The name.\">");
            RDataType dt = (RDataType) model.rootElements().get(0);

            assertEquals("Standalone", dt.name());
            assertEquals(Optional.empty(), dt.superTypeName());
            assertEquals(Optional.of("No parent."), dt.definition());

            RAttribute attr = dt.attributes().get(0);
            assertEquals("name", attr.name());
            assertEquals(Optional.of("The name."), attr.definition());
            assertEquals(BigInteger.ZERO, attr.cardinality().get().inf());
            assertEquals(BigInteger.ONE, attr.cardinality().get().sup());
        }

        @Test
        void parseDataTypeMultipleAttributes() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Trade:\n"
                    + "    tradeId string (1..1)\n"
                    + "    parties Party (0..*)\n"
                    + "    amount number (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertEquals(3, dt.attributes().size());
            assertEquals("tradeId", dt.attributes().get(0).name());
            assertEquals("parties", dt.attributes().get(1).name());
            assertEquals("amount", dt.attributes().get(2).name());
        }

        @Test
        void parseDataTypeAttributeWithOverride() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Child extends Parent:\n"
                    + "    override name string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertTrue(attr.isOverride());
            assertEquals("name", attr.name());
        }

        @Test
        void parseDataTypeNoAttributes() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Empty:");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertEquals("Empty", dt.name());
            assertTrue(dt.attributes().isEmpty());
        }

        // -- Enumeration tests -----------------------------------------------

        @Test
        void parseEnumerationMinimal() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "enum MyEnum:\n"
                    + "    VAL1\n"
                    + "    VAL2");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(REnumeration.class, model.rootElements().get(0));

            REnumeration en = (REnumeration) model.rootElements().get(0);
            assertEquals("MyEnum", en.name());
            assertEquals(Optional.empty(), en.superTypeName());
            assertEquals(Optional.empty(), en.definition());
            assertEquals(2, en.values().size());
            assertEquals("VAL1", en.values().get(0).name());
            assertEquals("VAL2", en.values().get(1).name());
        }

        @Test
        void parseEnumerationWithDisplayNameAndDefinition() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "enum MyEnum:\n"
                    + "    VAL1 displayName \"Value One\" <\"First value.\">\n"
                    + "    VAL2");
            REnumeration en = (REnumeration) model.rootElements().get(0);
            assertEquals(2, en.values().size());

            REnumValue val1 = en.values().get(0);
            assertEquals("VAL1", val1.name());
            assertEquals(Optional.of("Value One"), val1.displayName());
            assertEquals(Optional.of("First value."), val1.definition());

            REnumValue val2 = en.values().get(1);
            assertEquals("VAL2", val2.name());
            assertEquals(Optional.empty(), val2.displayName());
            assertEquals(Optional.empty(), val2.definition());
        }

        @Test
        void parseEnumerationExtending() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "enum ChildEnum extends ParentEnum: <\"A child enum.\">\n"
                    + "    Extra <\"An extra value.\">");
            REnumeration en = (REnumeration) model.rootElements().get(0);
            assertEquals("ChildEnum", en.name());
            assertEquals(Optional.of("ParentEnum"), en.superTypeName());
            assertEquals(Optional.of("A child enum."), en.definition());
            assertEquals(1, en.values().size());
            assertEquals("Extra", en.values().get(0).name());
            assertEquals(Optional.of("An extra value."), en.values().get(0).definition());
        }

        @Test
        void parseEnumerationNoValues() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "enum EmptyEnum:");
            REnumeration en = (REnumeration) model.rootElements().get(0);
            assertEquals("EmptyEnum", en.name());
            assertTrue(en.values().isEmpty());
        }

        // -- Choice tests ----------------------------------------------------

        @Test
        void parseChoiceMinimal() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "choice MyChoice:\n"
                    + "    Foo\n"
                    + "    Bar");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RChoice.class, model.rootElements().get(0));

            RChoice ch = (RChoice) model.rootElements().get(0);
            assertEquals("MyChoice", ch.name());
            assertEquals(Optional.empty(), ch.definition());
            assertEquals(2, ch.options().size());
            assertEquals("Foo", ch.options().get(0).typeCall().typeName());
            assertEquals("Bar", ch.options().get(1).typeCall().typeName());
        }

        @Test
        void parseChoiceWithDefinition() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "choice PayoutBase: <\"A payout choice.\">\n"
                    + "    CashPayout\n"
                    + "    PhysicalPayout");
            RChoice ch = (RChoice) model.rootElements().get(0);
            assertEquals("PayoutBase", ch.name());
            assertEquals(Optional.of("A payout choice."), ch.definition());
            assertEquals(2, ch.options().size());
        }

        @Test
        void parseChoiceNoOptions() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "choice EmptyChoice:");
            RChoice ch = (RChoice) model.rootElements().get(0);
            assertEquals("EmptyChoice", ch.name());
            assertTrue(ch.options().isEmpty());
        }

        // -- TypeAlias tests -------------------------------------------------

        @Test
        void parseTypeAliasMinimal() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "typeAlias positiveInt: <\"A positive integer.\"> int(min: 0)");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RTypeAlias.class, model.rootElements().get(0));

            RTypeAlias ta = (RTypeAlias) model.rootElements().get(0);
            assertEquals("positiveInt", ta.name());
            assertEquals(Optional.of("A positive integer."), ta.definition());
            assertNotNull(ta.typeCall());
            assertEquals("int", ta.typeCall().typeName());

            // Verify type call arguments
            assertEquals(1, ta.typeCall().arguments().size());
            RTypeCallArgument arg = ta.typeCall().arguments().get(0);
            assertEquals("min", arg.parameterName());
            assertNotNull(arg.value());
            assertEquals(Optional.of("0"), arg.value().literalValue());
        }

        @Test
        void parseTypeAliasWithoutDefinition() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "typeAlias myStr: string");
            RTypeAlias ta = (RTypeAlias) model.rootElements().get(0);
            assertEquals("myStr", ta.name());
            assertEquals(Optional.empty(), ta.definition());
            assertEquals("string", ta.typeCall().typeName());
            assertTrue(ta.typeCall().arguments().isEmpty());
        }

        // -- BasicType tests -------------------------------------------------

        @Test
        void parseBasicTypeMinimal() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "basicType myBool <\"A boolean.\">");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RBasicType.class, model.rootElements().get(0));

            RBasicType bt = (RBasicType) model.rootElements().get(0);
            assertEquals("myBool", bt.name());
            assertEquals(Optional.of("A boolean."), bt.definition());
            assertTrue(bt.typeParameters().isEmpty());
        }

        @Test
        void parseBasicTypeWithTypeParameters() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "basicType myNum(digits int, fractionalDigits int) <\"A number.\">");
            RBasicType bt = (RBasicType) model.rootElements().get(0);
            assertEquals("myNum", bt.name());
            assertEquals(Optional.of("A number."), bt.definition());
            assertEquals(2, bt.typeParameters().size());

            RTypeParameter tp1 = bt.typeParameters().get(0);
            assertEquals("digits", tp1.name());
            assertEquals("int", tp1.typeCall().typeName());

            RTypeParameter tp2 = bt.typeParameters().get(1);
            assertEquals("fractionalDigits", tp2.name());
            assertEquals("int", tp2.typeCall().typeName());
        }

        @Test
        void parseBasicTypeNoDefinition() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "basicType myBool");
            RBasicType bt = (RBasicType) model.rootElements().get(0);
            assertEquals("myBool", bt.name());
            assertEquals(Optional.empty(), bt.definition());
        }

        // -- RecordType tests ------------------------------------------------

        @Test
        void parseRecordTypeMinimal() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "recordType myRecord { <\"A record.\">\n"
                    + "    field1 int\n"
                    + "    field2 string\n"
                    + "}");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RRecordType.class, model.rootElements().get(0));

            RRecordType rt = (RRecordType) model.rootElements().get(0);
            assertEquals("myRecord", rt.name());
            assertEquals(Optional.of("A record."), rt.definition());
            assertEquals(2, rt.features().size());

            assertEquals("field1", rt.features().get(0).name());
            assertEquals("int", rt.features().get(0).typeCall().typeName());
            assertEquals("field2", rt.features().get(1).name());
            assertEquals("string", rt.features().get(1).typeCall().typeName());
        }

        @Test
        void parseRecordTypeEmpty() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "recordType emptyRec {}");
            RRecordType rt = (RRecordType) model.rootElements().get(0);
            assertEquals("emptyRec", rt.name());
            assertTrue(rt.features().isEmpty());
        }

        // -- MetaType tests --------------------------------------------------

        @Test
        void parseMetaType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "metaType reference string");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RMetaType.class, model.rootElements().get(0));

            RMetaType mt = (RMetaType) model.rootElements().get(0);
            assertEquals("reference", mt.name());
            assertNotNull(mt.typeCall());
            assertEquals("string", mt.typeCall().typeName());
        }

        // -- LibraryFunction tests -------------------------------------------

        @Test
        void parseLibraryFunctionMinimal() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "library function Max(a number, b number) number <\"Returns the max.\">");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RLibraryFunction.class, model.rootElements().get(0));

            RLibraryFunction lf = (RLibraryFunction) model.rootElements().get(0);
            assertEquals("Max", lf.name());
            assertEquals(Optional.of("Returns the max."), lf.definition());
            assertNotNull(lf.returnType());
            assertEquals("number", lf.returnType().typeName());
            assertEquals(2, lf.parameters().size());

            assertEquals("a", lf.parameters().get(0).name());
            assertEquals("number", lf.parameters().get(0).typeCall().typeName());
            assertFalse(lf.parameters().get(0).isArray());

            assertEquals("b", lf.parameters().get(1).name());
            assertEquals("number", lf.parameters().get(1).typeCall().typeName());
            assertFalse(lf.parameters().get(1).isArray());
        }

        @Test
        void parseLibraryFunctionWithArrayParam() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "library function Sum(items number[]) number <\"Sums items.\">");
            RLibraryFunction lf = (RLibraryFunction) model.rootElements().get(0);
            assertEquals("Sum", lf.name());
            assertEquals(1, lf.parameters().size());

            RParameter param = lf.parameters().get(0);
            assertEquals("items", param.name());
            assertEquals("number", param.typeCall().typeName());
            assertTrue(param.isArray());
        }

        @Test
        void parseLibraryFunctionNoParams() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "library function Now() dateTime <\"Returns current time.\">");
            RLibraryFunction lf = (RLibraryFunction) model.rootElements().get(0);
            assertEquals("Now", lf.name());
            assertTrue(lf.parameters().isEmpty());
            assertEquals("dateTime", lf.returnType().typeName());
        }

        // -- TypeCall with arguments tests -----------------------------------

        @Test
        void parseTypeCallWithArguments() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    amount number(digits: 5, fractionalDigits: 2) (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertEquals("number", attr.typeCall().typeName());
            assertEquals(2, attr.typeCall().arguments().size());

            RTypeCallArgument arg1 = attr.typeCall().arguments().get(0);
            assertEquals("digits", arg1.parameterName());
            assertEquals(Optional.of("5"), arg1.value().literalValue());
            assertFalse(arg1.value().isNegated());

            RTypeCallArgument arg2 = attr.typeCall().arguments().get(1);
            assertEquals("fractionalDigits", arg2.parameterName());
            assertEquals(Optional.of("2"), arg2.value().literalValue());
        }

        // -- Cardinality pattern tests ---------------------------------------

        @Test
        void parseCardinalityPatterns() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Cards:\n"
                    + "    a string (0..1)\n"
                    + "    b string (1..1)\n"
                    + "    c string (0..*)\n"
                    + "    d string (1..*)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertEquals(4, dt.attributes().size());

            // (0..1)
            RCardinality c0 = dt.attributes().get(0).cardinality().get();
            assertEquals(BigInteger.ZERO, c0.inf());
            assertEquals(BigInteger.ONE, c0.sup());
            assertFalse(c0.isUnbounded());

            // (1..1)
            RCardinality c1 = dt.attributes().get(1).cardinality().get();
            assertEquals(BigInteger.ONE, c1.inf());
            assertEquals(BigInteger.ONE, c1.sup());
            assertFalse(c1.isUnbounded());

            // (0..*)
            RCardinality c2 = dt.attributes().get(2).cardinality().get();
            assertEquals(BigInteger.ZERO, c2.inf());
            assertEquals(RCardinality.UNBOUNDED, c2.sup());
            assertTrue(c2.isUnbounded());

            // (1..*)
            RCardinality c3 = dt.attributes().get(3).cardinality().get();
            assertEquals(BigInteger.ONE, c3.inf());
            assertEquals(RCardinality.UNBOUNDED, c3.sup());
            assertTrue(c3.isUnbounded());
        }

        // -- Source range tests ----------------------------------------------

        @Test
        void sourceRangeSetOnDataType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertSourceRangeSet(dt);
        }

        @Test
        void sourceRangeSetOnAttributes() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertSourceRangeSet(dt.attributes().get(0));
        }

        @Test
        void sourceRangeSetOnTypeCall() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertSourceRangeSet(dt.attributes().get(0).typeCall());
        }

        @Test
        void sourceRangeSetOnCardinality() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertSourceRangeSet(dt.attributes().get(0).cardinality().get());
        }

        @Test
        void sourceRangeSetOnEnumeration() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "enum MyEnum:\n"
                    + "    VAL1");
            REnumeration en = (REnumeration) model.rootElements().get(0);
            assertSourceRangeSet(en);
            assertSourceRangeSet(en.values().get(0));
        }

        @Test
        void sourceRangeSetOnChoice() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "choice MyChoice:\n"
                    + "    Foo");
            RChoice ch = (RChoice) model.rootElements().get(0);
            assertSourceRangeSet(ch);
            assertSourceRangeSet(ch.options().get(0));
            assertSourceRangeSet(ch.options().get(0).typeCall());
        }

        @Test
        void sourceRangeSetOnTypeAlias() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "typeAlias myStr: string");
            RTypeAlias ta = (RTypeAlias) model.rootElements().get(0);
            assertSourceRangeSet(ta);
            assertSourceRangeSet(ta.typeCall());
        }

        @Test
        void sourceRangeSetOnBasicType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "basicType myBool");
            RBasicType bt = (RBasicType) model.rootElements().get(0);
            assertSourceRangeSet(bt);
        }

        @Test
        void sourceRangeSetOnRecordType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "recordType myRec { field1 int }");
            RRecordType rt = (RRecordType) model.rootElements().get(0);
            assertSourceRangeSet(rt);
            assertSourceRangeSet(rt.features().get(0));
            assertSourceRangeSet(rt.features().get(0).typeCall());
        }

        @Test
        void sourceRangeSetOnMetaType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "metaType reference string");
            RMetaType mt = (RMetaType) model.rootElements().get(0);
            assertSourceRangeSet(mt);
            assertSourceRangeSet(mt.typeCall());
        }

        @Test
        void sourceRangeSetOnLibraryFunction() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "library function Max(a number) number");
            RLibraryFunction lf = (RLibraryFunction) model.rootElements().get(0);
            assertSourceRangeSet(lf);
            assertSourceRangeSet(lf.parameters().get(0));
            assertSourceRangeSet(lf.returnType());
        }

        // -- Token range tests -----------------------------------------------

        @Test
        void tokenRangesSetOnDataType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo extends Bar: <\"A foo.\">\n"
                    + "    baz int (0..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertNotNull(dt.tokenRanges().get("keyword"));
            assertNotNull(dt.tokenRanges().get("name"));
            assertNotNull(dt.tokenRanges().get("extends"));
            assertNotNull(dt.tokenRanges().get("superType"));
            assertNotNull(dt.tokenRanges().get("colon"));
        }

        @Test
        void tokenRangesSetOnEnumeration() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "enum MyEnum extends ParentEnum:");
            REnumeration en = (REnumeration) model.rootElements().get(0);
            assertNotNull(en.tokenRanges().get("keyword"));
            assertNotNull(en.tokenRanges().get("name"));
            assertNotNull(en.tokenRanges().get("extends"));
            assertNotNull(en.tokenRanges().get("superType"));
        }

        @Test
        void tokenRangesSetOnAttribute() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    override bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertNotNull(attr.tokenRanges().get("override"));
            assertNotNull(attr.tokenRanges().get("name"));
        }

        @Test
        void tokenRangesSetOnTypeCall() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RTypeCall tc = dt.attributes().get(0).typeCall();
            assertNotNull(tc.tokenRanges().get("typeName"));
        }

        // -- Parent pointer tests --------------------------------------------

        @Test
        void parentPointerOnDataType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertSame(model, dt.parent());
        }

        @Test
        void parentPointerOnAttribute() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertSame(dt, attr.parent());
        }

        @Test
        void parentPointerOnTypeCallFromAttribute() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertSame(attr, attr.typeCall().parent());
        }

        @Test
        void parentPointerOnCardinalityFromAttribute() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            assertSame(attr, attr.cardinality().get().parent());
        }

        @Test
        void parentPointerOnEnumValue() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "enum MyEnum:\n"
                    + "    VAL1");
            REnumeration en = (REnumeration) model.rootElements().get(0);
            assertSame(en, en.values().get(0).parent());
        }

        @Test
        void parentPointerOnChoiceOption() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "choice MyChoice:\n"
                    + "    Foo");
            RChoice ch = (RChoice) model.rootElements().get(0);
            assertSame(ch, ch.options().get(0).parent());
            assertSame(ch.options().get(0), ch.options().get(0).typeCall().parent());
        }

        @Test
        void parentPointerOnRecordFeature() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "recordType myRec { f1 int }");
            RRecordType rt = (RRecordType) model.rootElements().get(0);
            assertSame(rt, rt.features().get(0).parent());
        }

        @Test
        void parentPointerOnLibraryFunctionParam() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "library function Max(a number) number");
            RLibraryFunction lf = (RLibraryFunction) model.rootElements().get(0);
            assertSame(lf, lf.parameters().get(0).parent());
            assertSame(lf, lf.returnType().parent());
        }

        @Test
        void parentPointerOnTypeAliasTypeCall() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "typeAlias myStr: string");
            RTypeAlias ta = (RTypeAlias) model.rootElements().get(0);
            assertSame(ta, ta.typeCall().parent());
        }

        // -- Deep tree tests -------------------------------------------------

        @Test
        void parseTypeCallArgumentsDeep() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    amount number(digits: 5, fractionalDigits: 2) (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            RTypeCall tc = dt.attributes().get(0).typeCall();

            // Type call arguments have source ranges and parent pointers
            assertSourceRangeSet(tc);
            for (RTypeCallArgument arg : tc.arguments()) {
                assertSourceRangeSet(arg);
                assertSame(tc, arg.parent());
                assertSourceRangeSet(arg.value());
                assertSame(arg, arg.value().parent());
            }
        }

        @Test
        void parseBasicTypeParametersDeep() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "basicType myNum(digits int, fractionalDigits int)");
            RBasicType bt = (RBasicType) model.rootElements().get(0);
            for (RTypeParameter tp : bt.typeParameters()) {
                assertSourceRangeSet(tp);
                assertSame(bt, tp.parent());
                assertSourceRangeSet(tp.typeCall());
                assertSame(tp, tp.typeCall().parent());
            }
        }

        // -- Multiple root elements ------------------------------------------

        @Test
        void parseMultipleRootElements() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "enum MyEnum:\n"
                    + "    VAL1\n"
                    + "choice MyChoice:\n"
                    + "    Foo\n"
                    + "    Bar");
            assertEquals(3, model.rootElements().size());
            assertInstanceOf(RDataType.class, model.rootElements().get(0));
            assertInstanceOf(REnumeration.class, model.rootElements().get(1));
            assertInstanceOf(RChoice.class, model.rootElements().get(2));

            // All have correct parent
            for (var elem : model.rootElements()) {
                assertSame(model, elem.parent());
            }
        }

        // -- Annotation/synonym/condition lists empty for now ----------------

        @Test
        void annotationListsEmptyForNow() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(0);
            assertTrue(dt.docReferences().isEmpty());
            assertTrue(dt.annotationRefs().isEmpty());
            assertTrue(dt.classSynonyms().isEmpty());
            assertTrue(dt.conditions().isEmpty());

            RAttribute attr = dt.attributes().get(0);
            assertTrue(attr.docReferences().isEmpty());
            assertTrue(attr.annotationRefs().isEmpty());
            assertTrue(attr.synonyms().isEmpty());
            assertTrue(attr.labelAnnotations().isEmpty());
            assertTrue(attr.ruleReferenceAnnotations().isEmpty());
        }

        @Test
        void enumAnnotationListsEmptyForNow() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "enum MyEnum:\n"
                    + "    VAL1");
            REnumeration en = (REnumeration) model.rootElements().get(0);
            assertTrue(en.docReferences().isEmpty());
            assertTrue(en.annotationRefs().isEmpty());
            assertTrue(en.synonyms().isEmpty());

            REnumValue val = en.values().get(0);
            assertTrue(val.docReferences().isEmpty());
            assertTrue(val.annotationRefs().isEmpty());
            assertTrue(val.synonyms().isEmpty());
        }

        @Test
        void parseDataTypeWithCondition() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "    condition BarNotEmpty:\n"
                    + "        bar exists");
            var dt = (com.regnosys.rosetta.ast.types.RDataType) model.rootElements().get(0);
            // Condition is parsed (expression is null until Task 13)
            assertEquals(1, dt.conditions().size());
            var cond = dt.conditions().get(0);
            assertTrue(cond.name().isPresent());
            assertEquals("BarNotEmpty", cond.name().get());
            assertSame(dt, cond.parent());
        }

        // -- Task 14 wiring verification: synonym lists populated -----------

        @Test
        void parseDataTypeWithClassSynonym_wiredInTask14() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    [synonym FpML value \"Foo\" meta \"id\"]\n"
                    + "    bar string (1..1)");
            var dt = (com.regnosys.rosetta.ast.types.RDataType) model.rootElements().get(0);
            assertEquals(1, dt.classSynonyms().size());
            assertSame(dt, dt.classSynonyms().get(0).parent());
        }

        @Test
        void parseAttributeWithSynonym_wiredInTask14() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\"]");
            var dt = (com.regnosys.rosetta.ast.types.RDataType) model.rootElements().get(0);
            var attr = dt.attributes().get(0);
            assertEquals(1, attr.synonyms().size());
            assertSame(attr, attr.synonyms().get(0).parent());
        }

        @Test
        void parseEnumValueWithSynonym_wiredInTask14() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "enum MyEnum:\n"
                    + "    VAL1\n"
                    + "        [synonym FpML value \"V1\"]");
            var en = (com.regnosys.rosetta.ast.types.REnumeration) model.rootElements().get(0);
            var val = en.values().get(0);
            assertEquals(1, val.synonyms().size());
            assertSame(val, val.synonyms().get(0).parent());
        }

        @Test
        void parseChoiceWithSynonyms_wiredInTask14() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "choice MyChoice:\n"
                    + "    [synonym FpML value \"ch\" meta \"id\"]\n"
                    + "    Foo\n"
                    + "        [synonym FpML value \"fooOpt\"]");
            var ch = (com.regnosys.rosetta.ast.types.RChoice) model.rootElements().get(0);
            assertEquals(1, ch.classSynonyms().size());
            assertSame(ch, ch.classSynonyms().get(0).parent());

            var opt = ch.options().get(0);
            assertEquals(1, opt.synonyms().size());
            assertSame(opt, opt.synonyms().get(0).parent());
        }
    }
}
