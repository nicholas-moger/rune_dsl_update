package com.regnosys.rosetta.types;

/**
 * Sealed interface for the M4 type model. Every Rune DSL type is one of
 * 9 variants. Java 21 sealed interfaces guarantee exhaustive {@code switch}
 * at compile time.
 *
 * <p>Spec: D3 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public sealed interface RType
    permits RDataTypeRef, REnumTypeRef, RChoiceTypeRef,
            RBasicType, RNumberType, RStringType,
            RRecordType, RAliasType, RMissingType {

    /** The type's name (e.g., "Trade", "boolean", "number", "MISSING"). */
    String name();

    /** Whether values of this type have a natural ordering. */
    boolean hasNaturalOrder();
}
