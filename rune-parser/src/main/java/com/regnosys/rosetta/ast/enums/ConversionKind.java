package com.regnosys.rosetta.ast.enums;

/**
 * Target type for conversion (to-xxx) expressions.
 *
 * <p>Grammar: {@code TO_NUMBER}, {@code TO_INT}, {@code TO_TIME},
 * {@code TO_ENUM}, {@code TO_DATE}, {@code TO_DATE_TIME}, {@code TO_ZONED_DATE_TIME}.
 */
public enum ConversionKind {
    NUMBER,
    INT,
    TIME,
    ENUM,
    DATE,
    DATE_TIME,
    ZONED_DATE_TIME
}
