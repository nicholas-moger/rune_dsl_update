package com.regnosys.rosetta.symbols.index;

import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;

import java.util.*;

/**
 * Inverted index: super type → direct subtypes. Powers
 * {@code RWorkspace.getSubTypes}. Built by GlobalResolutionPass as
 * super-type references are resolved.
 *
 * <p>Spec: D6 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class SubTypeIndex {

    private final Map<RDataType, List<RDataType>> dataSubTypes = new IdentityHashMap<>();
    private final Map<REnumeration, List<REnumeration>> enumSubTypes = new IdentityHashMap<>();

    public void registerInheritance(RDataType subType, RDataType superType) {
        dataSubTypes.computeIfAbsent(superType, k -> new ArrayList<>()).add(subType);
    }

    public void registerInheritance(REnumeration subType, REnumeration superType) {
        enumSubTypes.computeIfAbsent(superType, k -> new ArrayList<>()).add(subType);
    }

    public List<RDataType> getSubTypes(RDataType type) {
        return List.copyOf(dataSubTypes.getOrDefault(type, List.of()));
    }

    public List<REnumeration> getSubTypes(REnumeration type) {
        return List.copyOf(enumSubTypes.getOrDefault(type, List.of()));
    }
}
