package com.regnosys.rosetta.generator.java.types;

import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaParameterizedType;
import com.rosetta.util.types.JavaPrimitiveType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaTypeUtilTest {

    private final JavaTypeUtil util = new JavaTypeUtil();

    // === Type constants ======================================================

    @Test void string_constant() {
        assertEquals("String", util.STRING.getSimpleName());
    }

    @Test void boolean_constant() {
        assertEquals("Boolean", util.BOOLEAN.getSimpleName());
    }

    // === isList ==============================================================

    @Test void list_type_detected() {
        var listOfString = JavaParameterizedType.from(util.LIST, util.STRING);
        assertTrue(util.isList(listOfString));
    }

    @Test void non_list_not_detected() {
        assertFalse(util.isList(util.STRING));
    }

    // === isMapperS / isMapperC ===============================================

    @Test void mapper_s_detected() {
        var mapperS = JavaParameterizedType.from(util.MAPPER_S, util.STRING);
        assertTrue(util.isMapperS(mapperS));
        assertFalse(util.isMapperC(mapperS));
    }

    @Test void mapper_c_detected() {
        var mapperC = JavaParameterizedType.from(util.MAPPER_C, util.STRING);
        assertTrue(util.isMapperC(mapperC));
        assertFalse(util.isMapperS(mapperC));
    }

    // === isMapper (base Mapper interface exactly, not MapperS/MapperC) ========

    @Test void base_mapper_detected_by_isMapper() {
        var mapper = JavaParameterizedType.from(util.MAPPER, util.STRING);
        assertTrue(util.isMapper(mapper), "Mapper<String> should be detected by isMapper()");
    }

    @Test void mapper_s_not_detected_by_isMapper() {
        var mapperS = JavaParameterizedType.from(util.MAPPER_S, util.STRING);
        assertFalse(util.isMapper(mapperS), "MapperS should NOT be detected by isMapper()");
    }

    @Test void mapper_c_not_detected_by_isMapper() {
        var mapperC = JavaParameterizedType.from(util.MAPPER_C, util.STRING);
        assertFalse(util.isMapper(mapperC), "MapperC should NOT be detected by isMapper()");
    }

    // === isWrapper ============================================================

    @Test void list_is_wrapper() {
        var listOfString = JavaParameterizedType.from(util.LIST, util.STRING);
        assertTrue(util.isWrapper(listOfString));
    }

    @Test void mapper_is_wrapper() {
        var mapperS = JavaParameterizedType.from(util.MAPPER_S, util.STRING);
        assertTrue(util.isWrapper(mapperS));
    }

    @Test void comparison_result_is_wrapper() {
        // ComparisonResult is a concrete class (not parameterized) but isWrapper() must return true.
        assertTrue(util.isWrapper(util.COMPARISON_RESULT),
                "ComparisonResult should be recognised as a wrapper by isWrapper()");
    }

    @Test void plain_type_not_wrapper() {
        assertFalse(util.isWrapper(util.STRING));
    }

    // === getItemType =========================================================

    @Test void item_type_from_list() {
        var listOfString = JavaParameterizedType.from(util.LIST, util.STRING);
        assertEquals(util.STRING, util.getItemType(listOfString));
    }

    @Test void item_type_from_non_wrapper() {
        assertEquals(util.STRING, util.getItemType(util.STRING));
    }

    @Test void comparison_result_item_is_boolean() {
        assertEquals(util.BOOLEAN, util.getItemType(util.COMPARISON_RESULT));
    }

    // === isComparisonResult ==================================================

    @Test void comparison_result_detected() {
        assertTrue(util.isComparisonResult(util.COMPARISON_RESULT));
    }

    @Test void non_comparison_result() {
        assertFalse(util.isComparisonResult(util.STRING));
    }

    // === Number type checks ==================================================

    @Test void integer_detected() {
        assertTrue(util.isInteger(util.INTEGER));
        assertFalse(util.isLong(util.INTEGER));
    }

    @Test void big_decimal_detected() {
        assertTrue(util.isBigDecimal(util.BIG_DECIMAL));
        assertTrue(util.extendsNumber(util.BIG_DECIMAL));
    }

    // === Builder mapping =====================================================

    @Test void rosetta_model_object_has_builder() {
        assertTrue(util.hasBuilderType(util.ROSETTA_MODEL_OBJECT));
    }

    @Test void string_has_no_builder() {
        assertFalse(util.hasBuilderType(util.STRING));
    }

    // === wrap / wrapExtends ==================================================

    @Test void wrap_produces_parameterized_type() {
        var wrapped = util.wrap(util.LIST, util.STRING);
        assertTrue(util.isList(wrapped));
        assertEquals(util.STRING, util.getItemType(wrapped));
    }

    // === join ================================================================

    @Test void join_identical_types_returns_same() {
        assertEquals(util.STRING, util.join(util.STRING, util.STRING));
    }

    @Test void join_subtype_with_supertype_returns_supertype() {
        // Integer is a subtype of Number; join should return Number.
        assertEquals(util.NUMBER, util.join(util.INTEGER, util.NUMBER));
    }

    @Test void join_unrelated_types_falls_back_to_common_interface() {
        // String and Integer both implement Serializable; the joiner finds that
        // as a common interface before falling all the way back to Object.
        assertEquals(util.SERIALIZABLE, util.join(util.STRING, util.INTEGER));
    }

    // === isRosettaModelObject ================================================

    @Test void rosetta_model_object_detected() {
        assertTrue(util.isRosettaModelObject(util.ROSETTA_MODEL_OBJECT));
    }

    @Test void string_not_model_object() {
        assertFalse(util.isRosettaModelObject(util.STRING));
    }
}
