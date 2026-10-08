package com.regnosys.rosetta.generator.java.types;

/**
 * Meta-annotation types for POJO attributes. Controls generation of
 * {@code @RuneScopedAttributeKey} and {@code @RuneScopedAttributeReference}
 * annotations on generated getter methods.
 */
public enum AttributeMetaType {
    SCOPED_KEY,
    SCOPED_REFERENCE
}
