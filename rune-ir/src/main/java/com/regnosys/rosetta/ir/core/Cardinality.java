package com.regnosys.rosetta.ir.core;

/**
 * Field/parameter cardinality. DSL-agnostic — same vocabulary as common
 * modeling frameworks (EMF, Smithy).
 */
public enum Cardinality {
    /** Optional single value. UML {@code [0..1]}, EMF {@code lowerBound=0, upperBound=1}. */
    ZERO_TO_ONE,
    /** Optional collection. UML {@code [0..*]}, EMF {@code lowerBound=0, upperBound=-1}. */
    ZERO_TO_MANY,
    /** Required single value. UML {@code [1..1]}, EMF {@code lowerBound=1, upperBound=1}. */
    ONE_TO_ONE,
    /** Required non-empty collection. UML {@code [1..*]}, EMF {@code lowerBound=1, upperBound=-1}. */
    ONE_TO_MANY
}
