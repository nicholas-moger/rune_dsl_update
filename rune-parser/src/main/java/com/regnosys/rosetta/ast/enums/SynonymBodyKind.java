package com.regnosys.rosetta.ast.enums;

/**
 * Kind of synonym body, determining which alternative was matched
 * in the {@code rosettaSynonymBody} rule.
 *
 * <p>Grammar alternatives:
 * <ul>
 *   <li>{@code VALUE ...} — value synonym</li>
 *   <li>{@code HINT ...} — hint synonym</li>
 *   <li>{@code MERGE ...} — merge synonym</li>
 *   <li>{@code rosettaMappingSetTo} — set-to synonym</li>
 *   <li>{@code META ...} — meta-only synonym</li>
 * </ul>
 */
public enum SynonymBodyKind {
    VALUE,
    HINT,
    MERGE,
    SET_TO,
    META_ONLY
}
