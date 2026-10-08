package com.regnosys.rosetta.types.relation;

import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.types.*;

import java.util.*;

/**
 * Computes the least upper bound (join / LUB) of two types. Used by
 * type inference for conditionals, switch branches, and other contexts
 * where multiple types must be unified.
 *
 * <p>Spec: section 3.3 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public final class TypeJoin {

    private final SubtypeRelation subtypeRelation;

    public TypeJoin(SubtypeRelation subtypeRelation) {
        this.subtypeRelation = Objects.requireNonNull(subtypeRelation);
    }

    /** LUB of two types. */
    public RType join(RType t1, RType t2) {
        // Identity
        if (t1.equals(t2)) return t1;

        // Missing acts like bottom for join purposes
        if (t1 instanceof RMissingType) return t2;
        if (t2 instanceof RMissingType) return t1;

        // Bottom
        if (t1 instanceof RBasicType b && "nothing".equals(b.name())) return t2;
        if (t2 instanceof RBasicType b && "nothing".equals(b.name())) return t1;

        // Top
        if (t1 instanceof RBasicType b && "any".equals(b.name())) return RBasicType.ANY;
        if (t2 instanceof RBasicType b && "any".equals(b.name())) return RBasicType.ANY;

        // Alias transparency
        if (t1 instanceof RAliasType a) return join(a.refersTo(), t2);
        if (t2 instanceof RAliasType a) return join(t1, a.refersTo());

        // Number join — widen constraints (before subtype shortcut, since
        // Rule 4 says all numbers are subtypes of all numbers)
        if (t1 instanceof RNumberType n1 && t2 instanceof RNumberType n2) {
            return joinNumbers(n1, n2);
        }

        // String join — widen constraints (same reason as number)
        if (t1 instanceof RStringType s1 && t2 instanceof RStringType s2) {
            return joinStrings(s1, s2);
        }

        // Subtype shortcut: if one is subtype of the other, return the supertype
        if (subtypeRelation.isSubtypeOf(t1, t2)) return t2;
        if (subtypeRelation.isSubtypeOf(t2, t1)) return t1;

        // Data type join — find common ancestor
        if (t1 instanceof RDataTypeRef d1 && t2 instanceof RDataTypeRef d2) {
            return joinDataTypes(d1, d2);
        }

        // No common type — return any
        return RBasicType.ANY;
    }

    /** LUB of meta-annotated types. */
    public RMetaAnnotatedType join(RMetaAnnotatedType t1, RMetaAnnotatedType t2) {
        RType joined = join(t1.type(), t2.type());
        // Intersect meta attributes (only keep those present in both)
        List<String> commonMeta = new ArrayList<>(t1.metaAttributes());
        commonMeta.retainAll(t2.metaAttributes());
        return commonMeta.isEmpty()
            ? RMetaAnnotatedType.withNoMeta(joined)
            : RMetaAnnotatedType.withMeta(joined, commonMeta);
    }

    private RType joinNumbers(RNumberType n1, RNumberType n2) {
        // Widen: max of digits, max of fractionalDigits, union of ranges
        OptionalInt digits = maxOptInt(n1.digits(), n2.digits());
        OptionalInt frac = maxOptInt(n1.fractionalDigits(), n2.fractionalDigits());
        // For min/max bounds: widen = take the wider range
        var min = widenLower(n1.min(), n2.min());
        var max = widenUpper(n1.max(), n2.max());
        return new RNumberType(digits, frac, min, max);
    }

    private RType joinStrings(RStringType s1, RStringType s2) {
        // Widen: min of minLengths, max of maxLengths, drop pattern if different
        OptionalInt minLen = minOptInt(s1.minLength(), s2.minLength());
        OptionalInt maxLen = maxOptInt(s1.maxLength(), s2.maxLength());
        var pattern = s1.pattern().equals(s2.pattern()) ? s1.pattern() : java.util.Optional.<java.util.regex.Pattern>empty();
        return new RStringType(minLen, maxLen, pattern);
    }

    private RType joinDataTypes(RDataTypeRef d1, RDataTypeRef d2) {
        // Collect ancestor chains, find first common ancestor
        Set<RDataType> ancestors1 = ancestorChain(d1.astNode());
        RDataType current = d2.astNode();
        int safety = 0;
        while (current != null && safety++ < 1000) {
            if (ancestors1.contains(current)) {
                return new RDataTypeRef(current);
            }
            current = current.superType().orElse(null);
        }
        return RBasicType.ANY;
    }

    private Set<RDataType> ancestorChain(RDataType dt) {
        Set<RDataType> chain = Collections.newSetFromMap(new IdentityHashMap<>());
        RDataType current = dt;
        int safety = 0;
        while (current != null && safety++ < 1000) {
            chain.add(current);
            current = current.superType().orElse(null);
        }
        return chain;
    }

    private static OptionalInt maxOptInt(OptionalInt a, OptionalInt b) {
        // LUB widening: unconstrained (empty) on either side → unconstrained
        if (a.isEmpty() || b.isEmpty()) return OptionalInt.empty();
        return OptionalInt.of(Math.max(a.getAsInt(), b.getAsInt()));
    }

    private static OptionalInt minOptInt(OptionalInt a, OptionalInt b) {
        // LUB widening: unconstrained (empty) on either side → unconstrained
        if (a.isEmpty() || b.isEmpty()) return OptionalInt.empty();
        return OptionalInt.of(Math.min(a.getAsInt(), b.getAsInt()));
    }

    private static java.util.Optional<java.math.BigDecimal> widenLower(
            java.util.Optional<java.math.BigDecimal> a,
            java.util.Optional<java.math.BigDecimal> b) {
        if (a.isEmpty() || b.isEmpty()) return java.util.Optional.empty();
        return java.util.Optional.of(a.get().min(b.get()));
    }

    private static java.util.Optional<java.math.BigDecimal> widenUpper(
            java.util.Optional<java.math.BigDecimal> a,
            java.util.Optional<java.math.BigDecimal> b) {
        if (a.isEmpty() || b.isEmpty()) return java.util.Optional.empty();
        return java.util.Optional.of(a.get().max(b.get()));
    }
}
