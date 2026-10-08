package com.regnosys.rosetta.generator.java.types;

import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.rosetta.util.types.JavaReferenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Engine PR #9 / PR-B: the {@link RJavaWithMetaValue#create} factory that builds a
 * concrete per-namespace meta wrapper ({@link RJavaReferenceWithMeta} /
 * {@link RJavaFieldWithMeta}) from a bare value type, deriving the {@code metafields}
 * package the same way {@code RJavaPojoInterface.resolveWrappedTypeNamespace} +
 * {@code MetaFieldGenerator} do (the type's own namespace for generated types;
 * {@code com.rosetta.model} for builtins). This is the single construction site the
 * typed-pipeline navigation wiring uses to surface a real
 * {@code RJavaWithMetaValue} into a navigation receiver's expression type, so the
 * dormant {@code ItemToItemCoercer} meta-unwrap (PR-A) dispatches on it.
 */
class RJavaWithMetaValueTest {

    private JavaTypeUtil typeUtil;

    @BeforeEach
    void setUp() {
        typeUtil = new JavaTypeUtil();
    }

    /** A synthetic generated POJO named {@code Party} in {@code com.rosetta.test}. */
    private JavaReferenceType partyValueType() {
        return (JavaReferenceType) RGeneratedJavaClass.createWithSuperclass(
                JavaPackageName.splitOnDotsAndEscape("com.rosetta.test"), "Party",
                typeUtil.ROSETTA_MODEL_OBJECT);
    }

    @Test
    void create_reference_wrapper_for_generated_value_type_derives_metafields_package() {
        JavaReferenceType party = partyValueType();
        RJavaWithMetaValue meta = RJavaWithMetaValue.create(true, party, typeUtil);

        assertInstanceOf(RJavaReferenceWithMeta.class, meta);
        assertEquals("ReferenceWithMetaParty", meta.getSimpleName());
        assertEquals("com.rosetta.test.metafields", meta.getPackageName().withDots());
        assertSame(party, meta.getValueType());
    }

    @Test
    void create_field_wrapper_for_generated_value_type() {
        JavaReferenceType party = partyValueType();
        RJavaWithMetaValue meta = RJavaWithMetaValue.create(false, party, typeUtil);

        assertInstanceOf(RJavaFieldWithMeta.class, meta);
        assertEquals("FieldWithMetaParty", meta.getSimpleName());
        assertEquals("com.rosetta.test.metafields", meta.getPackageName().withDots());
        assertSame(party, meta.getValueType());
    }

    @Test
    void create_field_wrapper_for_builtin_value_type_uses_lib_namespace() {
        // String is a reflective JavaClass (NOT an RGeneratedJavaClass), so its meta
        // wrapper lives in com.rosetta.model.metafields — matching the golden import
        // `com.rosetta.model.metafields.FieldWithMetaString`.
        RJavaWithMetaValue meta = RJavaWithMetaValue.create(false, typeUtil.STRING, typeUtil);

        assertInstanceOf(RJavaFieldWithMeta.class, meta);
        assertEquals("FieldWithMetaString", meta.getSimpleName());
        assertEquals("com.rosetta.model.metafields", meta.getPackageName().withDots());
    }
}
