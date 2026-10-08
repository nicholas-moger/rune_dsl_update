package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.external.RExternalSynonymSource;
import com.regnosys.rosetta.ast.external.RExternalRuleSource;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.external.RExternalClass;
import com.regnosys.rosetta.ast.external.RExternalEnum;
import com.regnosys.rosetta.ast.mapping.RMapTestFunc;
import com.regnosys.rosetta.ast.regulatory.RSegmentRef;
import com.regnosys.rosetta.ast.functions.RDispatch;
import com.regnosys.rosetta.ast.model.RQualifiableConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the new M3 resolved-field accessors compile and return
 * Optional.empty() / empty list before linking. The linker tasks
 * (T5-T11) verify they return populated values after linking.
 */
class ResolvedFieldAccessorsTest {

    @Test void rDataType_superType_empty_before_linking() {
        assertTrue(new RDataType().superType().isEmpty());
    }

    @Test void rEnumeration_superType_empty_before_linking() {
        assertTrue(new REnumeration().superType().isEmpty());
    }

    @Test void rFunction_superFunction_empty_before_linking() {
        assertTrue(new RFunction().superFunction().isEmpty());
    }

    @Test void rTypeCall_referencedType_empty_before_linking() {
        assertTrue(new RTypeCall().referencedType().isEmpty());
    }

    @Test void rSymbolReference_symbol_empty_before_linking() {
        assertTrue(new RSymbolReference().symbol().isEmpty());
    }

    @Test void rAnnotationRef_annotation_empty_before_linking() {
        assertTrue(new RAnnotationRef().annotation().isEmpty());
    }

    @Test void rAnnotationRef_qualifier_empty_before_linking() {
        assertTrue(new RAnnotationRef().qualifier().isEmpty());
    }

    @Test void rRuleReferenceAnnotation_rule_empty_before_linking() {
        assertTrue(new RRuleReferenceAnnotation().rule().isEmpty());
    }

    @Test void rConversionExpr_targetEnum_empty_before_linking() {
        assertTrue(new RConversionExpr().targetEnum().isEmpty());
    }

    @Test void rQualifiableConfig_rootType_empty_before_linking() {
        assertTrue(new RQualifiableConfig().rootType().isEmpty());
    }

    @Test void rExternalClass_referencedType_empty_before_linking() {
        assertTrue(new RExternalClass().referencedType().isEmpty());
    }

    @Test void rExternalEnum_referencedType_empty_before_linking() {
        assertTrue(new RExternalEnum().referencedType().isEmpty());
    }

    @Test void rMapTestFunc_referencedFunction_empty_before_linking() {
        assertTrue(new RMapTestFunc().referencedFunction().isEmpty());
    }

    @Test void rEnumValueRef_enumeration_empty_before_linking() {
        assertTrue(new REnumValueRef().enumeration().isEmpty());
    }

    @Test void rEnumValueRef_enumValue_empty_before_linking() {
        assertTrue(new REnumValueRef().enumValue().isEmpty());
    }

    @Test void rSegmentRef_segment_empty_before_linking() {
        assertTrue(new RSegmentRef().segment().isEmpty());
    }

    @Test void rDispatch_parameter_empty_before_linking() {
        assertTrue(new RDispatch().parameter().isEmpty());
    }

    @Test void rDispatch_dispatchValue_empty_before_linking() {
        assertTrue(new RDispatch().dispatchValue().isEmpty());
    }

    @Test void rExternalSynonymSource_superSources_empty_before_linking() {
        assertTrue(new RExternalSynonymSource().superSources().isEmpty());
    }

    @Test void rExternalRuleSource_superSources_empty_before_linking() {
        assertTrue(new RExternalRuleSource().superSources().isEmpty());
    }
}
