package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.enums.MapPrimaryKind;
import com.regnosys.rosetta.ast.enums.MappingInstanceKind;
import com.regnosys.rosetta.ast.enums.SynonymBodyKind;
import com.regnosys.rosetta.ast.mapping.RMapPath;
import com.regnosys.rosetta.ast.mapping.RMapPathValue;
import com.regnosys.rosetta.ast.mapping.RMapPrimaryExpression;
import com.regnosys.rosetta.ast.mapping.RMapRosettaPath;
import com.regnosys.rosetta.ast.mapping.RMapTest;
import com.regnosys.rosetta.ast.mapping.RMapTestAbsent;
import com.regnosys.rosetta.ast.mapping.RMapTestEquality;
import com.regnosys.rosetta.ast.mapping.RMapTestExists;
import com.regnosys.rosetta.ast.mapping.RMapTestFunc;
import com.regnosys.rosetta.ast.mapping.RMapping;
import com.regnosys.rosetta.ast.mapping.RMappingInstance;
import com.regnosys.rosetta.ast.mapping.RMappingPathTests;
import com.regnosys.rosetta.ast.mapping.RMappingSetTo;
import com.regnosys.rosetta.ast.mapping.RMappingSetToInstance;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.synonyms.RSynonym;
import com.regnosys.rosetta.ast.synonyms.RSynonymBody;
import com.regnosys.rosetta.ast.types.RDataType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for mapping-related AST nodes (Task 7).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, list fields, and the {@code children()} traversal.
 */
class MappingNodeTest extends BaseAstTest {

    // =========================================================================
    // RMapPathValue
    // =========================================================================

    @Test
    void rMapPathValue_defaultState() {
        RMapPathValue pv = new RMapPathValue();
        assertNull(pv.value());
        assertTrue(pv.children().isEmpty());
    }

    @Test
    void rMapPathValue_setValue() {
        RMapPathValue pv = new RMapPathValue();
        pv.setValue("trade->header");
        assertEquals("trade->header", pv.value());
    }

    @Test
    void rMapPathValue_isLeafNode() {
        RMapPathValue pv = new RMapPathValue();
        pv.setValue("path");
        assertTrue(pv.children().isEmpty());
    }

    @Test
    void rMapPathValue_extendsRNode() {
        RMapPathValue pv = new RMapPathValue();
        assertInstanceOf(RNode.class, pv);
    }

    // =========================================================================
    // RMapPrimaryExpression
    // =========================================================================

    @Test
    void rMapPrimaryExpression_defaultState() {
        RMapPrimaryExpression expr = new RMapPrimaryExpression();
        assertNull(expr.kind());
        assertEquals(Optional.empty(), expr.enumRef());
        assertEquals(Optional.empty(), expr.stringValue());
        assertEquals(Optional.empty(), expr.boolValue());
        assertEquals(Optional.empty(), expr.intValue());
        assertEquals(Optional.empty(), expr.decimalValue());
    }

    @Test
    void rMapPrimaryExpression_enumValue() {
        RMapPrimaryExpression expr = new RMapPrimaryExpression();
        expr.setKind(MapPrimaryKind.ENUM_VALUE);
        expr.setEnumRef("TradeStatus.Active");

        assertEquals(MapPrimaryKind.ENUM_VALUE, expr.kind());
        assertEquals(Optional.of("TradeStatus.Active"), expr.enumRef());
    }

    @Test
    void rMapPrimaryExpression_stringValue() {
        RMapPrimaryExpression expr = new RMapPrimaryExpression();
        expr.setKind(MapPrimaryKind.STRING);
        expr.setStringValue("hello");

        assertEquals(MapPrimaryKind.STRING, expr.kind());
        assertEquals(Optional.of("hello"), expr.stringValue());
    }

    @Test
    void rMapPrimaryExpression_boolValue() {
        RMapPrimaryExpression expr = new RMapPrimaryExpression();
        expr.setKind(MapPrimaryKind.BOOLEAN);
        expr.setBoolValue(true);

        assertEquals(MapPrimaryKind.BOOLEAN, expr.kind());
        assertEquals(Optional.of(true), expr.boolValue());
    }

    @Test
    void rMapPrimaryExpression_intValue() {
        RMapPrimaryExpression expr = new RMapPrimaryExpression();
        expr.setKind(MapPrimaryKind.INT);
        expr.setIntValue(42);

        assertEquals(MapPrimaryKind.INT, expr.kind());
        assertEquals(Optional.of(BigInteger.valueOf(42)), expr.intValue());
    }

    @Test
    void rMapPrimaryExpression_decimalValue() {
        RMapPrimaryExpression expr = new RMapPrimaryExpression();
        expr.setKind(MapPrimaryKind.DECIMAL);
        expr.setDecimalValue("3.14");

        assertEquals(MapPrimaryKind.DECIMAL, expr.kind());
        assertEquals(Optional.of("3.14"), expr.decimalValue());
    }

    @Test
    void rMapPrimaryExpression_isLeafNode() {
        RMapPrimaryExpression expr = new RMapPrimaryExpression();
        expr.setKind(MapPrimaryKind.STRING);
        expr.setStringValue("test");
        assertTrue(expr.children().isEmpty());
    }

    // =========================================================================
    // RMapTest hierarchy
    // =========================================================================

    @Test
    void rMapTest_isAbstractBase() {
        // RMapTest is abstract, verify the hierarchy
        assertTrue(RMapTest.class.isAssignableFrom(RMapPath.class));
        assertTrue(RMapTest.class.isAssignableFrom(RMapRosettaPath.class));
        assertTrue(RMapTest.class.isAssignableFrom(RMapTestExists.class));
        assertTrue(RMapTest.class.isAssignableFrom(RMapTestAbsent.class));
        assertTrue(RMapTest.class.isAssignableFrom(RMapTestEquality.class));
        assertTrue(RMapTest.class.isAssignableFrom(RMapTestFunc.class));
        assertTrue(RNode.class.isAssignableFrom(RMapTest.class));
    }

    // =========================================================================
    // RMapPath
    // =========================================================================

    @Test
    void rMapPath_defaultState() {
        RMapPath mp = new RMapPath();
        assertNull(mp.pathValue());
        assertTrue(mp.children().isEmpty());
    }

    @Test
    void rMapPath_setPathValue() {
        RMapPath mp = new RMapPath();
        RMapPathValue pv = new RMapPathValue();
        pv.setValue("trade->id");
        mp.setPathValue(pv);

        assertSame(pv, mp.pathValue());
    }

    @Test
    void rMapPath_childrenIncludesPathValue() {
        RMapPath mp = new RMapPath();
        RMapPathValue pv = new RMapPathValue();
        pv.setValue("path");
        mp.setPathValue(pv);

        List<? extends RNode> children = mp.children();
        assertEquals(1, children.size());
        assertSame(pv, children.get(0));
    }

    @Test
    void rMapPath_extendsRMapTest() {
        RMapPath mp = new RMapPath();
        assertInstanceOf(RMapTest.class, mp);
        assertInstanceOf(RNode.class, mp);
    }

    // =========================================================================
    // RMapRosettaPath
    // =========================================================================

    @Test
    void rMapRosettaPath_defaultState() {
        RMapRosettaPath rp = new RMapRosettaPath();
        assertNull(rp.attributeReference());
        assertTrue(rp.children().isEmpty());
    }

    @Test
    void rMapRosettaPath_setAttributeReference() {
        RMapRosettaPath rp = new RMapRosettaPath();
        rp.setAttributeReference("Trade.tradeId");
        assertEquals("Trade.tradeId", rp.attributeReference());
    }

    @Test
    void rMapRosettaPath_isLeafNode() {
        RMapRosettaPath rp = new RMapRosettaPath();
        rp.setAttributeReference("ref");
        assertTrue(rp.children().isEmpty());
    }

    @Test
    void rMapRosettaPath_extendsRMapTest() {
        RMapRosettaPath rp = new RMapRosettaPath();
        assertInstanceOf(RMapTest.class, rp);
    }

    // =========================================================================
    // RMapTestExists
    // =========================================================================

    @Test
    void rMapTestExists_defaultState() {
        RMapTestExists te = new RMapTestExists();
        assertNull(te.pathValue());
        assertTrue(te.children().isEmpty());
    }

    @Test
    void rMapTestExists_setPathValue() {
        RMapTestExists te = new RMapTestExists();
        RMapPathValue pv = new RMapPathValue();
        pv.setValue("trade->status");
        te.setPathValue(pv);

        assertSame(pv, te.pathValue());
    }

    @Test
    void rMapTestExists_childrenIncludesPathValue() {
        RMapTestExists te = new RMapTestExists();
        RMapPathValue pv = new RMapPathValue();
        pv.setValue("path");
        te.setPathValue(pv);

        List<? extends RNode> children = te.children();
        assertEquals(1, children.size());
        assertSame(pv, children.get(0));
    }

    @Test
    void rMapTestExists_extendsRMapTest() {
        RMapTestExists te = new RMapTestExists();
        assertInstanceOf(RMapTest.class, te);
    }

    // =========================================================================
    // RMapTestAbsent
    // =========================================================================

    @Test
    void rMapTestAbsent_defaultState() {
        RMapTestAbsent ta = new RMapTestAbsent();
        assertNull(ta.pathValue());
        assertTrue(ta.children().isEmpty());
    }

    @Test
    void rMapTestAbsent_setPathValue() {
        RMapTestAbsent ta = new RMapTestAbsent();
        RMapPathValue pv = new RMapPathValue();
        pv.setValue("trade->optional");
        ta.setPathValue(pv);

        assertSame(pv, ta.pathValue());
    }

    @Test
    void rMapTestAbsent_childrenIncludesPathValue() {
        RMapTestAbsent ta = new RMapTestAbsent();
        RMapPathValue pv = new RMapPathValue();
        pv.setValue("path");
        ta.setPathValue(pv);

        List<? extends RNode> children = ta.children();
        assertEquals(1, children.size());
        assertSame(pv, children.get(0));
    }

    @Test
    void rMapTestAbsent_extendsRMapTest() {
        RMapTestAbsent ta = new RMapTestAbsent();
        assertInstanceOf(RMapTest.class, ta);
    }

    // =========================================================================
    // RMapTestEquality
    // =========================================================================

    @Test
    void rMapTestEquality_defaultState() {
        RMapTestEquality eq = new RMapTestEquality();
        assertNull(eq.pathValue());
        assertNull(eq.op());
        assertNull(eq.value());
        assertTrue(eq.children().isEmpty());
    }

    @Test
    void rMapTestEquality_equalityTest() {
        RMapTestEquality eq = new RMapTestEquality();

        RMapPathValue pv = new RMapPathValue();
        pv.setValue("trade->status");
        eq.setPathValue(pv);

        eq.setOp(EqOp.EQ);

        RMapPrimaryExpression val = new RMapPrimaryExpression();
        val.setKind(MapPrimaryKind.STRING);
        val.setStringValue("Active");
        eq.setValue(val);

        assertSame(pv, eq.pathValue());
        assertEquals(EqOp.EQ, eq.op());
        assertSame(val, eq.value());
    }

    @Test
    void rMapTestEquality_inequalityTest() {
        RMapTestEquality eq = new RMapTestEquality();

        RMapPathValue pv = new RMapPathValue();
        pv.setValue("type");
        eq.setPathValue(pv);

        eq.setOp(EqOp.NEQ);

        RMapPrimaryExpression val = new RMapPrimaryExpression();
        val.setKind(MapPrimaryKind.STRING);
        val.setStringValue("Cancelled");
        eq.setValue(val);

        assertEquals(EqOp.NEQ, eq.op());
    }

    @Test
    void rMapTestEquality_childrenIncludesPathAndValue() {
        RMapTestEquality eq = new RMapTestEquality();

        RMapPathValue pv = new RMapPathValue();
        pv.setValue("path");
        eq.setPathValue(pv);

        RMapPrimaryExpression val = new RMapPrimaryExpression();
        val.setKind(MapPrimaryKind.INT);
        val.setIntValue(1);
        eq.setValue(val);

        List<? extends RNode> children = eq.children();
        assertEquals(2, children.size());
        assertSame(pv, children.get(0));
        assertSame(val, children.get(1));
    }

    @Test
    void rMapTestEquality_childrenWithOnlyPath() {
        RMapTestEquality eq = new RMapTestEquality();
        RMapPathValue pv = new RMapPathValue();
        pv.setValue("path");
        eq.setPathValue(pv);

        List<? extends RNode> children = eq.children();
        assertEquals(1, children.size());
        assertSame(pv, children.get(0));
    }

    @Test
    void rMapTestEquality_extendsRMapTest() {
        RMapTestEquality eq = new RMapTestEquality();
        assertInstanceOf(RMapTest.class, eq);
    }

    // =========================================================================
    // RMapTestFunc
    // =========================================================================

    @Test
    void rMapTestFunc_defaultState() {
        RMapTestFunc tf = new RMapTestFunc();
        assertNull(tf.funcName());
        assertEquals(Optional.empty(), tf.conditionPath());
        assertTrue(tf.children().isEmpty());
    }

    @Test
    void rMapTestFunc_setFuncName() {
        RMapTestFunc tf = new RMapTestFunc();
        tf.setFuncName("IsValidTrade");
        assertEquals("IsValidTrade", tf.funcName());
    }

    @Test
    void rMapTestFunc_setConditionPath() {
        RMapTestFunc tf = new RMapTestFunc();
        tf.setFuncName("IsValid");

        RMapPathValue pv = new RMapPathValue();
        pv.setValue("trade->id");
        tf.setConditionPath(pv);

        assertTrue(tf.conditionPath().isPresent());
        assertSame(pv, tf.conditionPath().get());
    }

    @Test
    void rMapTestFunc_childrenIncludesConditionPath() {
        RMapTestFunc tf = new RMapTestFunc();
        tf.setFuncName("Check");

        RMapPathValue pv = new RMapPathValue();
        pv.setValue("p");
        tf.setConditionPath(pv);

        List<? extends RNode> children = tf.children();
        assertEquals(1, children.size());
        assertSame(pv, children.get(0));
    }

    @Test
    void rMapTestFunc_childrenEmptyWithoutConditionPath() {
        RMapTestFunc tf = new RMapTestFunc();
        tf.setFuncName("Check");
        assertTrue(tf.children().isEmpty());
    }

    @Test
    void rMapTestFunc_extendsRMapTest() {
        RMapTestFunc tf = new RMapTestFunc();
        assertInstanceOf(RMapTest.class, tf);
    }

    // =========================================================================
    // RMappingPathTests
    // =========================================================================

    @Test
    void rMappingPathTests_defaultState() {
        RMappingPathTests mpt = new RMappingPathTests();
        assertTrue(mpt.tests().isEmpty());
        assertTrue(mpt.children().isEmpty());
    }

    @Test
    void rMappingPathTests_addTests() {
        RMappingPathTests mpt = new RMappingPathTests();

        RMapTestExists exists = new RMapTestExists();
        RMapPathValue pv1 = new RMapPathValue();
        pv1.setValue("path1");
        exists.setPathValue(pv1);

        RMapTestAbsent absent = new RMapTestAbsent();
        RMapPathValue pv2 = new RMapPathValue();
        pv2.setValue("path2");
        absent.setPathValue(pv2);

        mpt.tests().add(exists);
        mpt.tests().add(absent);

        assertEquals(2, mpt.tests().size());
        assertInstanceOf(RMapTestExists.class, mpt.tests().get(0));
        assertInstanceOf(RMapTestAbsent.class, mpt.tests().get(1));
    }

    @Test
    void rMappingPathTests_childrenAreTests() {
        RMappingPathTests mpt = new RMappingPathTests();

        RMapRosettaPath rp = new RMapRosettaPath();
        rp.setAttributeReference("Trade.id");
        mpt.tests().add(rp);

        List<? extends RNode> children = mpt.children();
        assertEquals(1, children.size());
        assertSame(rp, children.get(0));
    }

    // =========================================================================
    // RMappingInstance
    // =========================================================================

    @Test
    void rMappingInstance_defaultState() {
        RMappingInstance mi = new RMappingInstance();
        assertNull(mi.kind());
        assertEquals(Optional.empty(), mi.tests());
        assertEquals(Optional.empty(), mi.defaultValue());
        assertTrue(mi.children().isEmpty());
    }

    @Test
    void rMappingInstance_setWhen() {
        RMappingInstance mi = new RMappingInstance();
        mi.setKind(MappingInstanceKind.SET_WHEN);

        RMappingPathTests tests = new RMappingPathTests();
        mi.setTests(tests);

        assertEquals(MappingInstanceKind.SET_WHEN, mi.kind());
        assertTrue(mi.tests().isPresent());
        assertEquals(Optional.empty(), mi.defaultValue());
    }

    @Test
    void rMappingInstance_defaultTo() {
        RMappingInstance mi = new RMappingInstance();
        mi.setKind(MappingInstanceKind.DEFAULT_TO);

        RMapPrimaryExpression val = new RMapPrimaryExpression();
        val.setKind(MapPrimaryKind.STRING);
        val.setStringValue("default");
        mi.setDefaultValue(val);

        assertEquals(MappingInstanceKind.DEFAULT_TO, mi.kind());
        assertTrue(mi.defaultValue().isPresent());
    }

    @Test
    void rMappingInstance_defaultToWithWhen() {
        RMappingInstance mi = new RMappingInstance();
        mi.setKind(MappingInstanceKind.DEFAULT_TO);

        RMapPrimaryExpression val = new RMapPrimaryExpression();
        val.setKind(MapPrimaryKind.BOOLEAN);
        val.setBoolValue(true);
        mi.setDefaultValue(val);

        RMappingPathTests tests = new RMappingPathTests();
        mi.setTests(tests);

        List<? extends RNode> children = mi.children();
        assertEquals(2, children.size());
        assertSame(tests, children.get(0));
        assertSame(val, children.get(1));
    }

    @Test
    void rMappingInstance_childrenForSetWhen() {
        RMappingInstance mi = new RMappingInstance();
        mi.setKind(MappingInstanceKind.SET_WHEN);

        RMappingPathTests tests = new RMappingPathTests();
        mi.setTests(tests);

        List<? extends RNode> children = mi.children();
        assertEquals(1, children.size());
        assertSame(tests, children.get(0));
    }

    // =========================================================================
    // RMapping
    // =========================================================================

    @Test
    void rMapping_defaultState() {
        RMapping m = new RMapping();
        assertTrue(m.instances().isEmpty());
        assertTrue(m.children().isEmpty());
    }

    @Test
    void rMapping_addInstances() {
        RMapping m = new RMapping();

        RMappingInstance mi1 = new RMappingInstance();
        mi1.setKind(MappingInstanceKind.SET_WHEN);

        RMappingInstance mi2 = new RMappingInstance();
        mi2.setKind(MappingInstanceKind.DEFAULT_TO);

        m.instances().add(mi1);
        m.instances().add(mi2);

        assertEquals(2, m.instances().size());
    }

    @Test
    void rMapping_childrenAreInstances() {
        RMapping m = new RMapping();

        RMappingInstance mi = new RMappingInstance();
        mi.setKind(MappingInstanceKind.SET_WHEN);
        m.instances().add(mi);

        List<? extends RNode> children = m.children();
        assertEquals(1, children.size());
        assertSame(mi, children.get(0));
    }

    // =========================================================================
    // RMappingSetTo
    // =========================================================================

    @Test
    void rMappingSetTo_defaultState() {
        RMappingSetTo mst = new RMappingSetTo();
        assertTrue(mst.instances().isEmpty());
        assertTrue(mst.children().isEmpty());
    }

    @Test
    void rMappingSetTo_addInstances() {
        RMappingSetTo mst = new RMappingSetTo();

        RMappingSetToInstance inst = new RMappingSetToInstance();
        RMapPrimaryExpression val = new RMapPrimaryExpression();
        val.setKind(MapPrimaryKind.STRING);
        val.setStringValue("test");
        inst.setValue(val);
        mst.instances().add(inst);

        assertEquals(1, mst.instances().size());
    }

    @Test
    void rMappingSetTo_childrenAreInstances() {
        RMappingSetTo mst = new RMappingSetTo();

        RMappingSetToInstance inst = new RMappingSetToInstance();
        RMapPrimaryExpression val = new RMapPrimaryExpression();
        val.setKind(MapPrimaryKind.INT);
        val.setIntValue(0);
        inst.setValue(val);
        mst.instances().add(inst);

        List<? extends RNode> children = mst.children();
        assertEquals(1, children.size());
        assertSame(inst, children.get(0));
    }

    // =========================================================================
    // RMappingSetToInstance
    // =========================================================================

    @Test
    void rMappingSetToInstance_defaultState() {
        RMappingSetToInstance msti = new RMappingSetToInstance();
        assertNull(msti.value());
        assertEquals(Optional.empty(), msti.when());
        assertTrue(msti.children().isEmpty());
    }

    @Test
    void rMappingSetToInstance_setValueOnly() {
        RMappingSetToInstance msti = new RMappingSetToInstance();
        RMapPrimaryExpression val = new RMapPrimaryExpression();
        val.setKind(MapPrimaryKind.STRING);
        val.setStringValue("hello");
        msti.setValue(val);

        assertSame(val, msti.value());
        assertEquals(Optional.empty(), msti.when());

        List<? extends RNode> children = msti.children();
        assertEquals(1, children.size());
        assertSame(val, children.get(0));
    }

    @Test
    void rMappingSetToInstance_setValueWithWhen() {
        RMappingSetToInstance msti = new RMappingSetToInstance();

        RMapPrimaryExpression val = new RMapPrimaryExpression();
        val.setKind(MapPrimaryKind.BOOLEAN);
        val.setBoolValue(false);
        msti.setValue(val);

        RMappingPathTests when = new RMappingPathTests();
        msti.setWhen(when);

        assertTrue(msti.when().isPresent());

        List<? extends RNode> children = msti.children();
        assertEquals(2, children.size());
        assertSame(val, children.get(0));
        assertSame(when, children.get(1));
    }

    // =========================================================================
    // Integration: full mapping tree
    // =========================================================================

    @Test
    void integration_setWhenEqualityMapping() {
        // [set when "trade->status" = "Active" and "trade->type" <> "Cancelled"]
        RMapping mapping = new RMapping();

        RMappingInstance mi = new RMappingInstance();
        mi.setKind(MappingInstanceKind.SET_WHEN);

        RMappingPathTests tests = new RMappingPathTests();

        // First test: "trade->status" = "Active"
        RMapTestEquality eq = new RMapTestEquality();
        RMapPathValue pv1 = new RMapPathValue();
        pv1.setValue("trade->status");
        eq.setPathValue(pv1);
        eq.setOp(EqOp.EQ);
        RMapPrimaryExpression val1 = new RMapPrimaryExpression();
        val1.setKind(MapPrimaryKind.STRING);
        val1.setStringValue("Active");
        eq.setValue(val1);
        tests.tests().add(eq);

        // Second test: "trade->type" <> "Cancelled"
        RMapTestEquality neq = new RMapTestEquality();
        RMapPathValue pv2 = new RMapPathValue();
        pv2.setValue("trade->type");
        neq.setPathValue(pv2);
        neq.setOp(EqOp.NEQ);
        RMapPrimaryExpression val2 = new RMapPrimaryExpression();
        val2.setKind(MapPrimaryKind.STRING);
        val2.setStringValue("Cancelled");
        neq.setValue(val2);
        tests.tests().add(neq);

        mi.setTests(tests);
        mapping.instances().add(mi);

        // Verify
        assertEquals(1, mapping.instances().size());
        RMappingInstance inst = mapping.instances().get(0);
        assertEquals(MappingInstanceKind.SET_WHEN, inst.kind());
        assertTrue(inst.tests().isPresent());
        assertEquals(2, inst.tests().get().tests().size());
        assertInstanceOf(RMapTestEquality.class, inst.tests().get().tests().get(0));
        assertInstanceOf(RMapTestEquality.class, inst.tests().get().tests().get(1));

        // children traversal
        List<? extends RNode> mappingChildren = mapping.children();
        assertEquals(1, mappingChildren.size());

        List<? extends RNode> instChildren = inst.children();
        assertEquals(1, instChildren.size());
        assertSame(tests, instChildren.get(0));

        List<? extends RNode> testsChildren = tests.children();
        assertEquals(2, testsChildren.size());
    }

    @Test
    void integration_defaultToWithWhen() {
        // [default to "fallback" when "path" exists]
        RMapping mapping = new RMapping();

        RMappingInstance mi = new RMappingInstance();
        mi.setKind(MappingInstanceKind.DEFAULT_TO);

        RMapPrimaryExpression defVal = new RMapPrimaryExpression();
        defVal.setKind(MapPrimaryKind.STRING);
        defVal.setStringValue("fallback");
        mi.setDefaultValue(defVal);

        RMappingPathTests tests = new RMappingPathTests();
        RMapTestExists exists = new RMapTestExists();
        RMapPathValue pv = new RMapPathValue();
        pv.setValue("path");
        exists.setPathValue(pv);
        tests.tests().add(exists);
        mi.setTests(tests);

        mapping.instances().add(mi);

        RMappingInstance result = mapping.instances().get(0);
        assertEquals(MappingInstanceKind.DEFAULT_TO, result.kind());
        assertTrue(result.defaultValue().isPresent());
        assertEquals(Optional.of("fallback"), result.defaultValue().get().stringValue());
        assertTrue(result.tests().isPresent());
        assertEquals(1, result.tests().get().tests().size());
    }

    @Test
    void integration_setToWithMultipleInstances() {
        // set to TradeStatus -> Active when "path" exists,
        //         TradeStatus -> Pending when "path" is absent
        RMappingSetTo setTo = new RMappingSetTo();

        // First instance
        RMappingSetToInstance inst1 = new RMappingSetToInstance();
        RMapPrimaryExpression val1 = new RMapPrimaryExpression();
        val1.setKind(MapPrimaryKind.ENUM_VALUE);
        val1.setEnumRef("TradeStatus.Active");
        inst1.setValue(val1);

        RMappingPathTests when1 = new RMappingPathTests();
        RMapTestExists exists = new RMapTestExists();
        RMapPathValue pv1 = new RMapPathValue();
        pv1.setValue("path");
        exists.setPathValue(pv1);
        when1.tests().add(exists);
        inst1.setWhen(when1);
        setTo.instances().add(inst1);

        // Second instance
        RMappingSetToInstance inst2 = new RMappingSetToInstance();
        RMapPrimaryExpression val2 = new RMapPrimaryExpression();
        val2.setKind(MapPrimaryKind.ENUM_VALUE);
        val2.setEnumRef("TradeStatus.Pending");
        inst2.setValue(val2);

        RMappingPathTests when2 = new RMappingPathTests();
        RMapTestAbsent absent = new RMapTestAbsent();
        RMapPathValue pv2 = new RMapPathValue();
        pv2.setValue("path");
        absent.setPathValue(pv2);
        when2.tests().add(absent);
        inst2.setWhen(when2);
        setTo.instances().add(inst2);

        // Verify
        assertEquals(2, setTo.instances().size());

        RMappingSetToInstance result1 = setTo.instances().get(0);
        assertEquals(MapPrimaryKind.ENUM_VALUE, result1.value().kind());
        assertEquals(Optional.of("TradeStatus.Active"), result1.value().enumRef());
        assertTrue(result1.when().isPresent());

        RMappingSetToInstance result2 = setTo.instances().get(1);
        assertEquals(Optional.of("TradeStatus.Pending"), result2.value().enumRef());
        assertTrue(result2.when().isPresent());

        // children traversal
        List<? extends RNode> setToChildren = setTo.children();
        assertEquals(2, setToChildren.size());
    }

    @Test
    void integration_funcTestInMapping() {
        // [set when IsValidTrade("trade->id")]
        RMappingPathTests tests = new RMappingPathTests();

        RMapTestFunc func = new RMapTestFunc();
        func.setFuncName("IsValidTrade");
        RMapPathValue condPath = new RMapPathValue();
        condPath.setValue("trade->id");
        func.setConditionPath(condPath);
        tests.tests().add(func);

        assertEquals(1, tests.tests().size());
        assertInstanceOf(RMapTestFunc.class, tests.tests().get(0));
        RMapTestFunc result = (RMapTestFunc) tests.tests().get(0);
        assertEquals("IsValidTrade", result.funcName());
        assertTrue(result.conditionPath().isPresent());
        assertEquals("trade->id", result.conditionPath().get().value());

        List<? extends RNode> funcChildren = func.children();
        assertEquals(1, funcChildren.size());
        assertSame(condPath, funcChildren.get(0));
    }

    @Test
    void integration_mixedMapTests() {
        // Combine different test types in one RMappingPathTests
        RMappingPathTests tests = new RMappingPathTests();

        RMapPath path = new RMapPath();
        RMapPathValue pv1 = new RMapPathValue();
        pv1.setValue("p1");
        path.setPathValue(pv1);
        tests.tests().add(path);

        RMapRosettaPath rPath = new RMapRosettaPath();
        rPath.setAttributeReference("Trade.id");
        tests.tests().add(rPath);

        RMapTestExists exists = new RMapTestExists();
        RMapPathValue pv2 = new RMapPathValue();
        pv2.setValue("p2");
        exists.setPathValue(pv2);
        tests.tests().add(exists);

        RMapTestAbsent absent = new RMapTestAbsent();
        RMapPathValue pv3 = new RMapPathValue();
        pv3.setValue("p3");
        absent.setPathValue(pv3);
        tests.tests().add(absent);

        RMapTestEquality eq = new RMapTestEquality();
        RMapPathValue pv4 = new RMapPathValue();
        pv4.setValue("p4");
        eq.setPathValue(pv4);
        eq.setOp(EqOp.EQ);
        RMapPrimaryExpression val = new RMapPrimaryExpression();
        val.setKind(MapPrimaryKind.INT);
        val.setIntValue(42);
        eq.setValue(val);
        tests.tests().add(eq);

        RMapTestFunc func = new RMapTestFunc();
        func.setFuncName("Check");
        tests.tests().add(func);

        assertEquals(6, tests.tests().size());
        assertInstanceOf(RMapPath.class, tests.tests().get(0));
        assertInstanceOf(RMapRosettaPath.class, tests.tests().get(1));
        assertInstanceOf(RMapTestExists.class, tests.tests().get(2));
        assertInstanceOf(RMapTestAbsent.class, tests.tests().get(3));
        assertInstanceOf(RMapTestEquality.class, tests.tests().get(4));
        assertInstanceOf(RMapTestFunc.class, tests.tests().get(5));

        // All tests are children
        List<? extends RNode> children = tests.children();
        assertEquals(6, children.size());
    }

    // =========================================================================
    // Integration: end-to-end parse-and-build tests (Task 14)
    // =========================================================================

    @Nested
    class IntegrationParseAndBuild {

        /**
         * Returns the single synonym body attached to the attribute {@code bar}
         * of the single data type {@code Foo} in the given parsed model.
         */
        private RSynonymBody firstBarSynonymBody(RModel model) {
            RDataType dt = (RDataType) model.rootElements().get(0);
            RAttribute attr = dt.attributes().get(0);
            RSynonym syn = attr.synonyms().get(0);
            return syn.body();
        }

        // -- Mapping: set-when form ------------------------------------------

        @Test
        void parseMapping_setWhenWithPathEquality() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " set when path = \"isActive\"]");
            RSynonymBody body = firstBarSynonymBody(model);

            assertTrue(body.mapping().isPresent());
            RMapping mapping = body.mapping().get();
            assertEquals(1, mapping.instances().size());

            RMappingInstance inst = mapping.instances().get(0);
            assertEquals(MappingInstanceKind.SET_WHEN, inst.kind());
            assertTrue(inst.tests().isPresent());
            assertEquals(1, inst.tests().get().tests().size());
            assertInstanceOf(RMapPath.class, inst.tests().get().tests().get(0));

            RMapPath mapPath = (RMapPath) inst.tests().get().tests().get(0);
            assertNotNull(mapPath.pathValue());
            assertEquals("isActive", mapPath.pathValue().value());
            assertSourceRangeSet(mapPath);
            assertSourceRangeSet(mapPath.pathValue());
        }

        @Test
        void parseMapping_setWhenAndedTests() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " set when \"p1\" exists and \"p2\" is absent]");
            RSynonymBody body = firstBarSynonymBody(model);
            RMapping mapping = body.mapping().get();

            RMappingInstance inst = mapping.instances().get(0);
            assertEquals(MappingInstanceKind.SET_WHEN, inst.kind());
            assertEquals(2, inst.tests().get().tests().size());
            assertInstanceOf(RMapTestExists.class, inst.tests().get().tests().get(0));
            assertInstanceOf(RMapTestAbsent.class, inst.tests().get().tests().get(1));

            RMapTestExists existsTest = (RMapTestExists) inst.tests().get().tests().get(0);
            assertEquals("p1", existsTest.pathValue().value());
            RMapTestAbsent absentTest = (RMapTestAbsent) inst.tests().get().tests().get(1);
            assertEquals("p2", absentTest.pathValue().value());
        }

        @Test
        void parseMapping_setWhenEqualityTest() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " set when \"p1\" = \"match\"]");
            RSynonymBody body = firstBarSynonymBody(model);
            RMappingInstance inst = body.mapping().get().instances().get(0);
            RMapTestEquality eq = (RMapTestEquality)
                    inst.tests().get().tests().get(0);

            assertEquals("p1", eq.pathValue().value());
            assertEquals(EqOp.EQ, eq.op());
            assertNotNull(eq.value());
            assertEquals(MapPrimaryKind.STRING, eq.value().kind());
            assertEquals(Optional.of("match"), eq.value().stringValue());
        }

        @Test
        void parseMapping_setWhenNeqEquality() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " set when \"p1\" <> 42]");
            RMappingInstance inst = firstBarSynonymBody(model).mapping().get().instances().get(0);
            RMapTestEquality eq = (RMapTestEquality) inst.tests().get().tests().get(0);

            assertEquals(EqOp.NEQ, eq.op());
            assertEquals(MapPrimaryKind.INT, eq.value().kind());
            assertEquals(Optional.of(BigInteger.valueOf(42)), eq.value().intValue());
        }

        @Test
        void parseMapping_setWhenRosettaPath() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " set when rosettaPath = Trade -> tradeId]");
            RMappingInstance inst = firstBarSynonymBody(model).mapping().get().instances().get(0);
            RMapRosettaPath rp = (RMapRosettaPath) inst.tests().get().tests().get(0);
            assertEquals("Trade -> tradeId", rp.attributeReference());
        }

        @Test
        void parseMapping_setWhenCondFunc() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " set when condition-func Check condition-path \"p1\"]");
            RMappingInstance inst = firstBarSynonymBody(model).mapping().get().instances().get(0);
            RMapTestFunc func = (RMapTestFunc) inst.tests().get().tests().get(0);
            assertEquals("Check", func.funcName());
            assertTrue(func.conditionPath().isPresent());
            assertEquals("p1", func.conditionPath().get().value());
        }

        @Test
        void parseMapping_setWhenCondFuncNoPath() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " set when condition-func Check]");
            RMappingInstance inst = firstBarSynonymBody(model).mapping().get().instances().get(0);
            RMapTestFunc func = (RMapTestFunc) inst.tests().get().tests().get(0);
            assertEquals("Check", func.funcName());
            assertFalse(func.conditionPath().isPresent());
        }

        // -- Mapping: default-to form ----------------------------------------

        @Test
        void parseMapping_defaultToStringValue() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " default to \"fallback\"]");
            RMappingInstance inst = firstBarSynonymBody(model).mapping().get().instances().get(0);

            assertEquals(MappingInstanceKind.DEFAULT_TO, inst.kind());
            assertTrue(inst.defaultValue().isPresent());
            assertEquals(MapPrimaryKind.STRING, inst.defaultValue().get().kind());
            assertEquals(Optional.of("fallback"), inst.defaultValue().get().stringValue());
        }

        @Test
        void parseMapping_defaultToBooleanValue() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " default to True]");
            RMappingInstance inst = firstBarSynonymBody(model).mapping().get().instances().get(0);
            assertEquals(MapPrimaryKind.BOOLEAN, inst.defaultValue().get().kind());
            assertEquals(Optional.of(Boolean.TRUE), inst.defaultValue().get().boolValue());
        }

        // -- Mapping: set-to standalone form ---------------------------------

        @Test
        void parseMapping_setToSingle() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML set to \"fixed\"]");
            RSynonymBody body = firstBarSynonymBody(model);
            assertEquals(SynonymBodyKind.SET_TO, body.kind());
            assertTrue(body.setTo().isPresent());
            RMappingSetTo setTo = body.setTo().get();
            assertEquals(1, setTo.instances().size());

            RMappingSetToInstance inst = setTo.instances().get(0);
            assertNotNull(inst.value());
            assertEquals(MapPrimaryKind.STRING, inst.value().kind());
            assertEquals(Optional.of("fixed"), inst.value().stringValue());
            assertFalse(inst.when().isPresent());
        }

        @Test
        void parseMapping_setToWithWhen() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML set to \"fixed\" when \"p\" exists]");
            RSynonymBody body = firstBarSynonymBody(model);
            RMappingSetToInstance inst = body.setTo().get().instances().get(0);
            assertTrue(inst.when().isPresent());
            assertEquals(1, inst.when().get().tests().size());
            assertInstanceOf(RMapTestExists.class, inst.when().get().tests().get(0));
        }

        // -- Source ranges + parent pointers ---------------------------------

        @Test
        void mappingNodes_haveSourceRangesAndParents() {
            RModel model = parseAndBuild(
                    "namespace test\n"
                    + "type Foo:\n"
                    + "    bar string (1..1)\n"
                    + "        [synonym FpML value \"barField\""
                    + " set when \"p1\" = \"v1\" and \"p2\" exists]");
            RSynonymBody body = firstBarSynonymBody(model);
            RMapping mapping = body.mapping().get();

            assertSourceRangeSet(mapping);
            assertSame(body, mapping.parent());

            RMappingInstance inst = mapping.instances().get(0);
            assertSourceRangeSet(inst);
            assertSame(mapping, inst.parent());

            assertSourceRangeSet(inst.tests().get());
            assertSame(inst, inst.tests().get().parent());

            for (RMapTest t : inst.tests().get().tests()) {
                assertSourceRangeSet(t);
                assertSame(inst.tests().get(), t.parent());
            }
        }
    }
}
