package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.expressions.references.*;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureCallTypeTest {

    private final ExpressionTypeComputer computer;
    private final TypeInferenceEngine engine;

    FeatureCallTypeTest() {
        var builtins = BuiltinTypeRegistry.createDefault();
        var sub = new SubtypeRelation();
        var join = new TypeJoin(sub);
        var alias = new TypeAliasSolver();
        computer = new ExpressionTypeComputer(builtins, sub, join, alias);
        engine = new TypeInferenceEngine(computer);
    }

    // === RSymbolReference resolved to RAttribute =============================

    @Test void symbol_ref_resolved_to_attribute_with_data_type() {
        var resolver = new TestSymbolResolver();
        var dt = new RDataType();
        dt.setName("Trade");
        dt.attachToWorkspace(resolver);
        resolver.bind("test", "Trade", dt);
        var tc = new RTypeCall();
        tc.setTypeName("Trade");
        tc.attachToWorkspace(resolver);
        tc.setReferencedTypeId(resolver.idFor("test", "Trade"));
        var attr = new RAttribute();
        attr.setName("trade");
        attr.setTypeCall(tc);

        var ref = new RSymbolReference();
        ref.setName("trade");
        ref.setResolvedSymbol(attr);

        RMetaAnnotatedType result = computer.compute(ref, engine);
        assertInstanceOf(RDataTypeRef.class, result.type());
        assertEquals("Trade", result.type().name());
        Reference.reachabilityFence(resolver);
    }

    @Test void symbol_ref_resolved_to_attribute_with_enum_type() {
        var resolver = new TestSymbolResolver();
        var en = new REnumeration();
        en.setName("Color");
        en.attachToWorkspace(resolver);
        resolver.bind("test", "Color", en);
        var tc = new RTypeCall();
        tc.setTypeName("Color");
        tc.attachToWorkspace(resolver);
        tc.setReferencedTypeId(resolver.idFor("test", "Color"));
        var attr = new RAttribute();
        attr.setName("color");
        attr.setTypeCall(tc);

        var ref = new RSymbolReference();
        ref.setName("color");
        ref.setResolvedSymbol(attr);

        RMetaAnnotatedType result = computer.compute(ref, engine);
        assertInstanceOf(REnumTypeRef.class, result.type());
        assertEquals("Color", result.type().name());
        Reference.reachabilityFence(resolver);
    }

    @Test void symbol_ref_resolved_to_attribute_with_builtin_type() {
        // Attribute typed as "int" → M3 resolves typeCall to null (builtins not in namespace)
        // But typeName is "int" → M4 looks up in BuiltinTypeRegistry
        var tc = new RTypeCall();
        tc.setTypeName("int");
        // referencedType is empty (builtins aren't in M3's namespace scope)
        var attr = new RAttribute();
        attr.setName("count");
        attr.setTypeCall(tc);

        var ref = new RSymbolReference();
        ref.setName("count");
        ref.setResolvedSymbol(attr);

        RMetaAnnotatedType result = computer.compute(ref, engine);
        assertInstanceOf(RNumberType.class, result.type());
        assertTrue(((RNumberType) result.type()).isInteger());
    }

    // === RSymbolReference resolved to RFunction ==============================

    @Test void symbol_ref_resolved_to_function() {
        var resolver = new TestSymbolResolver();
        var dt = new RDataType();
        dt.setName("Result");
        dt.attachToWorkspace(resolver);
        resolver.bind("test", "Result", dt);
        var tc = new RTypeCall();
        tc.setTypeName("Result");
        tc.attachToWorkspace(resolver);
        tc.setReferencedTypeId(resolver.idFor("test", "Result"));
        var outputAttr = new RAttribute();
        outputAttr.setName("result");
        outputAttr.setTypeCall(tc);

        var fn = new RFunction();
        fn.setName("Calculate");
        fn.setOutput(outputAttr);

        var ref = new RSymbolReference();
        ref.setName("Calculate");
        ref.setResolvedSymbol(fn);

        RMetaAnnotatedType result = computer.compute(ref, engine);
        assertInstanceOf(RDataTypeRef.class, result.type());
        assertEquals("Result", result.type().name());
        Reference.reachabilityFence(resolver);
    }

    // === Unresolved symbol → MISSING ========================================

    @Test void unresolved_symbol_returns_missing() {
        var ref = new RSymbolReference();
        ref.setName("unknown");
        // symbol() is empty — not resolved by M3
        RMetaAnnotatedType result = computer.compute(ref, engine);
        assertTrue(result.isMissing());
    }

    // === RFeatureCall with resolved feature ==================================

    @Test void feature_call_with_resolved_feature() {
        var resolver = new TestSymbolResolver();
        var dt = new RDataType();
        dt.setName("Price");
        dt.attachToWorkspace(resolver);
        resolver.bind("test", "Price", dt);
        var tc = new RTypeCall();
        tc.setTypeName("Price");
        tc.attachToWorkspace(resolver);
        tc.setReferencedTypeId(resolver.idFor("test", "Price"));
        var priceAttr = new RAttribute();
        priceAttr.setName("amount");
        priceAttr.setTypeCall(tc);

        var fc = new RFeatureCall();
        fc.setFeatureName("amount");
        fc.setResolvedFeature(priceAttr);

        RMetaAnnotatedType result = computer.compute(fc, engine);
        assertInstanceOf(RDataTypeRef.class, result.type());
        assertEquals("Price", result.type().name());
        Reference.reachabilityFence(resolver);
    }

    @Test void feature_call_without_resolved_feature_is_missing() {
        var fc = new RFeatureCall();
        fc.setFeatureName("unknown");
        // resolvedFeature not set
        RMetaAnnotatedType result = computer.compute(fc, engine);
        assertTrue(result.isMissing());
    }
}
