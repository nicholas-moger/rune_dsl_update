package com.regnosys.rosetta.types.relation;

import com.regnosys.rosetta.types.*;

import java.util.*;

/**
 * Subtype relation for the Rune DSL type system. 8 rules matching Xtext
 * exactly, plus alias transparency and cycle prevention.
 *
 * <p>Spec: section 3.3 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public final class SubtypeRelation {

    /** Checks whether t1 is a subtype of t2 (t1 <= t2). */
    public boolean isSubtypeOf(RType t1, RType t2) {
        return isSubtypeOf(t1, t2, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /** Meta-annotated convenience — compares underlying types. */
    public boolean isSubtypeOf(RMetaAnnotatedType t1, RMetaAnnotatedType t2) {
        return isSubtypeOf(t1.type(), t2.type());
    }

    private boolean isSubtypeOf(RType t1, RType t2, Set<Object> visited) {
        // Rule 1: Identity
        if (t1.equals(t2)) return true;

        // Rule 2: Bottom — nothing <= T
        if (t1 instanceof RBasicType b && "nothing".equals(b.name())) return true;

        // Rule 3: Top — T <= any
        if (t2 instanceof RBasicType b && "any".equals(b.name())) return true;

        // Alias transparency — unwrap before further comparison
        if (t1 instanceof RAliasType a) return isSubtypeOf(a.refersTo(), t2, visited);
        if (t2 instanceof RAliasType a) return isSubtypeOf(t1, a.refersTo(), visited);

        // Rule 4: Number — any number <= any number
        if (t1 instanceof RNumberType && t2 instanceof RNumberType) return true;

        // Rule 5: String — any string <= any string
        if (t1 instanceof RStringType && t2 instanceof RStringType) return true;

        // Rule 6: Data — S <= T if S.superType <= T (transitive)
        if (t1 instanceof RDataTypeRef d1 && t2 instanceof RDataTypeRef) {
            // Cycle prevention tracks AST node identity (wrapper objects differ)
            if (!visited.add(d1.astNode())) return false;
            var sup = d1.astNode().superType();
            if (sup.isEmpty()) return false;
            return isSubtypeOf(new RDataTypeRef(sup.get()), t2, visited);
        }

        // Rule 7: Choice LHS — choice{A,B} <= T if A <= T && B <= T
        // Each option gets an isolated visited set to prevent false cycle detection
        if (t1 instanceof RChoiceTypeRef c1) {
            return c1.options().stream().allMatch(o ->
                isSubtypeOf(o, t2, Collections.newSetFromMap(new IdentityHashMap<>())));
        }

        // Rule 8: Choice RHS — S <= choice{A,B} if S <= A || S <= B
        // Each option gets an isolated visited set
        if (t2 instanceof RChoiceTypeRef c2) {
            return c2.options().stream().anyMatch(o ->
                isSubtypeOf(t1, o, Collections.newSetFromMap(new IdentityHashMap<>())));
        }

        return false;
    }
}
