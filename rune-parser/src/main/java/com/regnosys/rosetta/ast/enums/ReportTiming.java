package com.regnosys.rosetta.ast.enums;

/**
 * Timing specification for report declarations.
 *
 * <p>Grammar: {@code IN (REAL_TIME | T_PLUS_1 | T_PLUS_2 | T_PLUS_3 | T_PLUS_4 | T_PLUS_5 | ASATP)}
 * (rosettaReport).
 */
public enum ReportTiming {
    REAL_TIME,
    T_PLUS_1,
    T_PLUS_2,
    T_PLUS_3,
    T_PLUS_4,
    T_PLUS_5,
    ASATP
}
