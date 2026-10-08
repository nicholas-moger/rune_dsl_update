package com.regnosys.rosetta.generator.java.types;

import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.enums.ReportTiming;
import com.regnosys.rosetta.ast.enums.RuleKind;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.regulatory.RRegulatoryDocumentReference;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.regnosys.rosetta.types.*;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class JavaTypeTranslatorTest {

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);

    // === Basic types ==========================================================

    @Test void boolean_maps_to_primitive_boolean() {
        assertEquals(JavaPrimitiveType.BOOLEAN, translator.toJavaType(RBasicType.BOOLEAN));
    }

    @Test void time_maps_to_local_time() {
        assertEquals(typeUtil.LOCAL_TIME, translator.toJavaType(RBasicType.TIME));
    }

    @Test void nothing_maps_to_void() {
        assertEquals(typeUtil.VOID, translator.toJavaType(RBasicType.NOTHING));
    }

    @Test void any_maps_to_object() {
        assertEquals(typeUtil.OBJECT, translator.toJavaType(RBasicType.ANY));
    }

    @Test void pattern_throws_illegal_state() {
        assertThrows(IllegalStateException.class,
                () -> translator.toJavaType(RBasicType.PATTERN));
    }

    // === Number types =========================================================

    @Test void non_integer_number_maps_to_big_decimal() {
        var num = RNumberType.unconstrained();
        assertEquals(JavaClass.from(BigDecimal.class), translator.toJavaType(num));
    }

    @Test void integer_unconstrained_defaults_to_int() {
        // Default digits = 9, so maps to int
        var num = RNumberType.intType();
        assertEquals(JavaPrimitiveType.INT, translator.toJavaType(num));
    }

    @Test void integer_9_digits_maps_to_int() {
        var num = new RNumberType(OptionalInt.of(9), OptionalInt.of(0),
                Optional.empty(), Optional.empty());
        assertEquals(JavaPrimitiveType.INT, translator.toJavaType(num));
    }

    @Test void integer_10_digits_maps_to_long() {
        var num = new RNumberType(OptionalInt.of(10), OptionalInt.of(0),
                Optional.empty(), Optional.empty());
        assertEquals(JavaPrimitiveType.LONG, translator.toJavaType(num));
    }

    @Test void integer_18_digits_maps_to_long() {
        var num = new RNumberType(OptionalInt.of(18), OptionalInt.of(0),
                Optional.empty(), Optional.empty());
        assertEquals(JavaPrimitiveType.LONG, translator.toJavaType(num));
    }

    @Test void integer_19_digits_maps_to_big_integer() {
        var num = new RNumberType(OptionalInt.of(19), OptionalInt.of(0),
                Optional.empty(), Optional.empty());
        assertEquals(JavaClass.from(BigInteger.class), translator.toJavaType(num));
    }

    @Test void integer_100_digits_maps_to_big_integer() {
        var num = new RNumberType(OptionalInt.of(100), OptionalInt.of(0),
                Optional.empty(), Optional.empty());
        assertEquals(JavaClass.from(BigInteger.class), translator.toJavaType(num));
    }

    // === String type ==========================================================

    @Test void string_maps_to_string() {
        var str = RStringType.unconstrained();
        assertEquals(typeUtil.STRING, translator.toJavaType(str));
    }

    @Test void constrained_string_still_maps_to_string() {
        var str = new RStringType(OptionalInt.of(1), OptionalInt.of(100), Optional.empty());
        assertEquals(typeUtil.STRING, translator.toJavaType(str));
    }

    // === Record types =========================================================

    @Test void date_maps_to_rosetta_date() {
        assertEquals(typeUtil.DATE, translator.toJavaType(RRecordType.DATE));
    }

    @Test void date_time_maps_to_local_date_time() {
        assertEquals(typeUtil.LOCAL_DATE_TIME, translator.toJavaType(RRecordType.DATE_TIME));
    }

    @Test void zoned_date_time_maps_to_zoned_date_time() {
        assertEquals(typeUtil.ZONED_DATE_TIME, translator.toJavaType(RRecordType.ZONED_DATE_TIME));
    }

    // === Alias type ===========================================================

    @Test void alias_recurses_to_underlying_type() {
        var alias = new RAliasType("MyInt", Map.of(), RNumberType.intType());
        assertEquals(JavaPrimitiveType.INT, translator.toJavaType(alias));
    }

    @Test void nested_alias_recurses_fully() {
        var inner = new RAliasType("InnerAlias", Map.of(), RBasicType.BOOLEAN);
        var outer = new RAliasType("OuterAlias", Map.of(), inner);
        assertEquals(JavaPrimitiveType.BOOLEAN, translator.toJavaType(outer));
    }

    // === Missing type =========================================================

    @Test void missing_maps_to_object() {
        assertEquals(typeUtil.OBJECT, translator.toJavaType(RMissingType.INSTANCE));
    }

    // === Data type / Enum / Choice (placeholders until T4) ====================

    // === Detached model-type refs: REFUSED, not collapsed to Object ===========
    //
    // v3.1 phase C, C0 item 1. These three asserted that a model type whose declaring
    // RModel is unreachable translates to java.lang.Object. That was the fork's
    // behaviour and it is a GUESS: every consumer downstream then types against Object,
    // and nothing anywhere says so. Measured across the 25-cell band x 11 kinds x both
    // byte routes, the site fires ZERO times — no generated output depends on the
    // collapse — so C0 converted it to a refusal, and these tests now hold the refusal
    // contract instead of the collapse. The inputs below are synthetic (an AST node with
    // no parent model cannot come out of a parse); the point of keeping them is that the
    // API's answer to an unanswerable question is now "UNSUPPORTED, here", not "Object".

    @Test void detached_data_type_ref_refuses() {
        var dt = new RDataType(); dt.setName("Trade");
        var ref = new RDataTypeRef(dt);
        var refusal = assertThrows(SilentDegradation.Refusal.class,
                () -> translator.toJavaType(ref));
        assertTrue(refusal.getMessage().contains("Trade")
                        && refusal.getMessage().contains("TYPE_COLLAPSED_TO_OBJECT"),
                "the refusal must name the type and the site: " + refusal.getMessage());
    }

    @Test void detached_enum_type_ref_refuses() {
        var en = new REnumeration(); en.setName("Color");
        var ref = new REnumTypeRef(en);
        assertThrows(SilentDegradation.Refusal.class, () -> translator.toJavaType(ref));
    }

    @Test void detached_choice_type_ref_refuses() {
        var ref = new RChoiceTypeRef("MyChoice", List.of());
        assertThrows(SilentDegradation.Refusal.class, () -> translator.toJavaType(ref));
    }

    @Test void choice_type_ref_with_ast_returns_generated_class() {
        var model = new com.regnosys.rosetta.ast.model.RModel();
        model.setNamespace("com.example.product");
        var ch = new com.regnosys.rosetta.ast.types.RChoice();
        ch.setName("Underlier");
        ch.setParent(model);
        var ref = new RChoiceTypeRef(ch.name(), List.of(), ch);
        JavaType jt = translator.toJavaType(ref);
        assertTrue(jt instanceof RGeneratedJavaClass<?>,
                "Choice type with AST should return RGeneratedJavaClass");
        assertEquals("Underlier", jt.getSimpleName());
        assertTrue(jt.isSubtypeOf(typeUtil.ROSETTA_MODEL_OBJECT),
                "Choice type should be a RosettaModelObject");
    }

    // === toJavaReferenceType (boxes primitives) ===============================

    @Test void boolean_boxed_to_Boolean() {
        var ref = translator.toJavaReferenceType(RBasicType.BOOLEAN);
        assertEquals(JavaClass.from(Boolean.class), ref);
    }

    @Test void int_boxed_to_Integer() {
        var ref = translator.toJavaReferenceType(RNumberType.intType());
        assertEquals(JavaClass.from(Integer.class), ref);
    }

    @Test void string_ref_stays_String() {
        var ref = translator.toJavaReferenceType(RStringType.unconstrained());
        assertEquals(typeUtil.STRING, ref);
    }

    // === Phase X T2.5 — toLabelProviderJavaClass(ModelSymbolId) =================

    @Test
    void toLabelProviderJavaClass_appendsLabelProviderSuffix() {
        ModelSymbolId functionId =
                new ModelSymbolId(DottedPath.splitOnDots("com.example.reports"), "FooReport");

        RGeneratedJavaClass<? extends LabelProvider> labelClass =
                translator.toLabelProviderJavaClass(functionId);

        assertEquals("FooReportLabelProvider", labelClass.getSimpleName());
    }

    @Test
    void toLabelProviderJavaClass_putsClassInLabelsSubPackage() {
        ModelSymbolId functionId =
                new ModelSymbolId(DottedPath.splitOnDots("com.example.reports"), "FooReport");

        RGeneratedJavaClass<? extends LabelProvider> labelClass =
                translator.toLabelProviderJavaClass(functionId);

        // Upstream LabelProviderGenerator.xtend convention: <funcNamespace>.labels
        assertEquals("com.example.reports.labels",
                labelClass.getPackageName().withDots());
    }

    @Test
    void toLabelProviderJavaClass_isSubtypeOfLabelProvider() {
        ModelSymbolId functionId =
                new ModelSymbolId(DottedPath.splitOnDots("cdm.product.template"), "MyFunc");

        RGeneratedJavaClass<? extends LabelProvider> labelClass =
                translator.toLabelProviderJavaClass(functionId);

        // The generated class is declared as implementing LabelProvider so that
        // downstream typeUtil / Java-class machinery treats it correctly.
        assertTrue(labelClass.isSubtypeOf(JavaClass.from(LabelProvider.class)));
    }

    // === Phase X T2.5 — toMetaJavaType(RAttribute, RType) =======================

    @Test
    void toMetaJavaType_returnsBareReferenceTypeWhenNoMetaAnnotation() {
        RAttribute attr = new RAttribute();
        attr.setName("plainAttr");
        // No @metadata annotationRef added → bare path.

        JavaType result = translator.toMetaJavaType(attr, RBasicType.BOOLEAN);

        assertEquals(JavaClass.from(Boolean.class), result,
                "Plain attribute should map to its reference java type (boxed primitive).");
    }

    @Test
    void toMetaJavaType_returnsBareTypeForStringWithoutMeta() {
        RAttribute attr = new RAttribute();
        attr.setName("name");

        JavaType result = translator.toMetaJavaType(attr, RStringType.unconstrained());

        assertEquals(typeUtil.STRING, result);
    }

    @Test
    void toMetaJavaType_returnsWrapperMarkerWhenMetaAnnotationPresent() {
        RAttribute attr = new RAttribute();
        attr.setName("metaAttr");
        RAnnotationRef metaRef = new RAnnotationRef();
        metaRef.setAnnotationName("metadata");
        attr.annotationRefs().add(metaRef);

        JavaType result = translator.toMetaJavaType(attr, RStringType.unconstrained());

        // Meta-annotated attributes return a JavaType whose simpleName carries the
        // FieldWithMeta marker — the concrete per-namespace wrapper class
        // (e.g. FieldWithMetaString) is composed downstream by the
        // Rule/Report/LabelProvider generators (T5/T6) that have GeneratorModel
        // in scope; T2.5 returns the marker so callers can detect the meta path.
        assertNotNull(result);
        assertTrue(result.getSimpleName().contains("FieldWithMeta"),
                "Meta-annotated attribute should surface a FieldWithMeta marker; got: "
                        + result.getSimpleName());
    }

    @Test
    void toMetaJavaType_metadataAnnotationOnBooleanYieldsWrapperMarker() {
        // Exercises the boxed-primitive (Boolean) meta path: hasMetaAnnotation
        // returns true on a [metadata]-annotated attribute, so the FieldWithMeta
        // wrapper is returned over the boxed Boolean.
        //
        // Note: the fork's hasMetaAnnotation matches "metadata" ONLY — byte-parity
        // with legacy plugin's FunctionGenerator.hasMeta at 9.83.0-line upstream. Upstream's
        // getRMetaAnnotatedType extends the meta surface to `reference` /
        // `location` / `scheme` annotations as well; T3/T5/T6 generator
        // implementers will surface any real `reference`-attribute byte-diff
        // through D11 byte-diff at T7/T8 (the legacy-plugin parity invariant
        // pins this subset; any divergence proves the subset is wrong).
        RAttribute attr = new RAttribute();
        attr.setName("refAttr");
        RAnnotationRef metaRef = new RAnnotationRef();
        metaRef.setAnnotationName("metadata");
        attr.annotationRefs().add(metaRef);

        JavaType result = translator.toMetaJavaType(attr, RBasicType.BOOLEAN);

        assertNotNull(result);
        assertTrue(result.getSimpleName().contains("FieldWithMeta"));
    }

    // === Phase X T6.0.5 — origin-dispatched toFunctionJavaClass(RFunction) ====

    // FUNCTION origin (default — directly-constructed RFunction). Routes to
    // <namespace>.functions/<Name> per upstream lines 117-121.
    @Test
    void toFunctionJavaClass_originFunction_routesToFunctionsPackage() {
        RFunction func = new RFunction();
        func.setName("Calculate");
        // origin defaults to Origin.FUNCTION per T6.0.5.

        ModelSymbolId functionId = new ModelSymbolId(
                DottedPath.splitOnDots("com.example"), "Calculate");

        RGeneratedJavaClass<? extends RosettaFunction> clazz =
                translator.toFunctionJavaClass(func, functionId);

        assertEquals("Calculate", clazz.getSimpleName(),
                "FUNCTION origin: simple name must be the function name");
        assertEquals("com.example.functions",
                clazz.getPackageName().withDots(),
                "FUNCTION origin: package must be <namespace>.functions");
    }

    // RULE origin. Routes to <namespace>.reports/<Name>Rule per upstream
    // lines 127-131. Pre-T6.0.5 RuleGenerator incorrectly routed rules to
    // <namespace>.functions/<Name> via the FUNCTION-origin path — this test
    // locks the correct dispatch.
    @Test
    void toFunctionJavaClass_originRule_routesToReportsPackageWithRuleSuffix() {
        // Build a synthetic RRule + bridge to RFunction via fromRule so
        // origin=RULE is set.
        RRule rule = new RRule();
        rule.setKind(RuleKind.REPORTING);
        rule.setName("TradeIdRule");
        RFunction func = RFunction.fromRule(rule);

        ModelSymbolId functionId = new ModelSymbolId(
                DottedPath.splitOnDots("com.example"), "TradeIdRule");

        RGeneratedJavaClass<? extends RosettaFunction> clazz =
                translator.toFunctionJavaClass(func, functionId);

        assertEquals("TradeIdRuleRule", clazz.getSimpleName(),
                "RULE origin: simple name must be <ruleName>Rule (upstream "
                + "appends Rule suffix verbatim — synthetic name "
                + "'TradeIdRule' yields 'TradeIdRuleRule' here; real corpora "
                + "use names that don't end in Rule)");
        assertEquals("com.example.reports",
                clazz.getPackageName().withDots(),
                "RULE origin: package must be <namespace>.reports (NOT .functions "
                + "as the pre-T6.0.5 T5 RuleGenerator incorrectly routed)");
    }

    // REPORT origin. Routes to <namespace>.reports/<body+corpus>ReportFunction
    // per upstream lines 122-126. Replaces the ad-hoc
    // toReportFunctionJavaClass(DottedPath, RReport) entry point.
    @Test
    void toFunctionJavaClass_originReport_routesToReportsPackageWithReportFunctionSuffix() {
        // Build a synthetic RReport + bridge to RFunction via fromReport so
        // origin=REPORT is set + originReport is populated.
        RReport report = new RReport();
        report.setTiming(ReportTiming.T_PLUS_1);
        RRegulatoryDocumentReference docRef = new RRegulatoryDocumentReference();
        docRef.setBodyRef("ASIC");
        docRef.corpusRefs().add("Margin");
        report.setRegulatoryDocRef(docRef);
        RTypeCall inputTc = new RTypeCall();
        inputTc.setTypeName("Trade");
        report.setFromType(inputTc);
        report.setWithType("ASICMarginReport");

        RFunction func = RFunction.fromReport(report);

        // ModelSymbolId carries the namespace; the simple-name is derived
        // from the originReport's body+corpus by the private toJavaReportClass
        // router, NOT from functionId.getName().
        ModelSymbolId functionId = new ModelSymbolId(
                DottedPath.splitOnDots("drr.regulation.asic.rewrite.margin"),
                "ignored");

        RGeneratedJavaClass<? extends RosettaFunction> clazz =
                translator.toFunctionJavaClass(func, functionId);

        assertEquals("ASICMarginReportFunction", clazz.getSimpleName(),
                "REPORT origin: simple name must be "
                + "<body><corpus...>ReportFunction (body='ASIC' + corpus="
                + "['Margin'] → 'ASICMarginReportFunction')");
        assertEquals("drr.regulation.asic.rewrite.margin.reports",
                clazz.getPackageName().withDots(),
                "REPORT origin: package must be <namespace>.reports");
    }

    // BC entry point — pre-T6.0.5 call sites (15+ in fork) that already have
    // a ModelSymbolId in hand must continue to work via the 1-arg overload.
    // Delegates internally to the FUNCTION-origin path.
    @Test
    void toFunctionJavaClass_modelSymbolIdOverload_routesToFunctionsPackage() {
        ModelSymbolId functionId = new ModelSymbolId(
                DottedPath.splitOnDots("com.example"), "BcCallSite");

        RGeneratedJavaClass<? extends RosettaFunction> clazz =
                translator.toFunctionJavaClass(functionId);

        assertEquals("BcCallSite", clazz.getSimpleName(),
                "BC overload must preserve simple name");
        assertEquals("com.example.functions",
                clazz.getPackageName().withDots(),
                "BC overload must preserve <namespace>.functions routing");
    }

    // === Exhaustiveness check =================================================

    @Test void all_9_variants_handled() {
        // Ensure every RType variant is HANDLED (no missing case). v3.1 C0: the three
        // model-type refs are constructed DETACHED here (no declaring RModel), which is
        // now a refusal rather than a silent Object — so they are asserted separately
        // just below, and the "translates without throwing" list covers the rest.
        for (RType detached : List.<RType>of(
                new RDataTypeRef(new RDataType()),
                new REnumTypeRef(new REnumeration()),
                new RChoiceTypeRef("C", List.of()))) {
            assertThrows(SilentDegradation.Refusal.class, () -> translator.toJavaType(detached),
                    "a detached model-type ref must refuse, not collapse to Object: " + detached);
        }
        var types = List.<RType>of(
                RBasicType.BOOLEAN,
                RBasicType.TIME,
                RBasicType.NOTHING,
                RBasicType.ANY,
                RNumberType.unconstrained(),
                RNumberType.intType(),
                RStringType.unconstrained(),
                RRecordType.DATE,
                RRecordType.DATE_TIME,
                RRecordType.ZONED_DATE_TIME,
                new RAliasType("A", Map.of(), RBasicType.BOOLEAN),
                RMissingType.INSTANCE
        );
        for (RType type : types) {
            assertDoesNotThrow(() -> translator.toJavaType(type),
                    "Failed to translate: " + type);
        }
    }
}
