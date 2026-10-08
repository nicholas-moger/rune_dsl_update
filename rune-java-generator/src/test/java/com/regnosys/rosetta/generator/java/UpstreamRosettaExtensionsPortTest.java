package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../object/RosettaExtensionsTest.xtend} — 3/3
 * methods. Upstream's {@code RDataType.allSuperTypes} /
 * {@code REnumType.allParents} / {@code allEnumValues} map to the fork's
 * {@code superType()} resolution chains: the super walks are SELF-INCLUSIVE and
 * cycle-BOUNDED (the {@code visited.add} loop pattern the generator's own walks
 * use), and the all-values walk is {@code GeneratorModel.allValues} (inherited
 * first, own appended — the upstream order). The cycle case is the upstream
 * robustness observable: a Foo◄Bar◄Baz◄Foo extends-cycle must terminate with
 * each type seeing all three. Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamRosettaExtensionsPortTest {

    private static RWorkspace parse(String source) {
        RModel model = AstBuilder.buildFromString(source, "extensions-port.rosetta");
        return RWorkspace.build(List.of(model)).workspace();
    }

    /** The self-inclusive, cycle-bounded super-type walk (upstream allSuperTypes). */
    private static Set<RDataType> allSuperTypes(RDataType t) {
        Set<RDataType> seen = new LinkedHashSet<>();
        for (RDataType cur = t; cur != null && seen.add(cur); cur = cur.superType().orElse(null)) {
            // walk
        }
        return seen;
    }

    /** The enum twin (upstream allParents — also self-inclusive upstream). */
    private static Set<REnumeration> allParents(REnumeration e) {
        Set<REnumeration> seen = new LinkedHashSet<>();
        for (REnumeration cur = e; cur != null && seen.add(cur); cur = cur.superType().orElse(null)) {
            // walk
        }
        return seen;
    }

    private static List<RDataType> dataTypes(RWorkspace ws) {
        List<RDataType> out = new ArrayList<>();
        for (var el : ws.files().get(0).rootElements()) {
            if (el instanceof RDataType dt) {
                out.add(dt);
            }
        }
        return out;
    }

    /** Upstream {@code testSuperClasses}. */
    @Test
    void testSuperClasses() {
        RWorkspace ws = parse("""
                namespace test

                type Foo extends Bar:
                type Bar extends Baz:
                type Baz:
                """);
        List<RDataType> classes = dataTypes(ws);
        assertEquals(Set.copyOf(classes), allSuperTypes(classes.get(0)));
        assertEquals(Set.of(classes.get(1), classes.get(2)), allSuperTypes(classes.get(1)));
        assertEquals(Set.of(classes.get(2)), allSuperTypes(classes.get(2)));
    }

    /** Upstream {@code testSuperClassesWithCycle} — the walk must terminate. */
    @Test
    void testSuperClassesWithCycle() {
        RWorkspace ws = parse("""
                namespace test

                type Foo extends Bar:
                type Bar extends Baz:
                type Baz extends Foo:
                """);
        List<RDataType> classes = dataTypes(ws);
        assertEquals(Set.copyOf(classes), allSuperTypes(classes.get(0)));
        assertEquals(Set.copyOf(classes), allSuperTypes(classes.get(1)));
        assertEquals(Set.copyOf(classes), allSuperTypes(classes.get(2)));
    }

    /** Upstream {@code testEnumValue}. */
    @Test
    void testEnumValue() {
        RWorkspace ws = parse("""
                namespace test
                version "1.2.3"

                enum Foo:
                	foo0 foo1

                enum Bar extends Foo:
                	bar
                enum Baz extends Bar:
                	baz
                """);
        List<REnumeration> enums = new ArrayList<>();
        for (var el : ws.files().get(0).rootElements()) {
            if (el instanceof REnumeration e) {
                enums.add(e);
            }
        }
        REnumeration foo = enums.get(0);
        REnumeration bar = enums.get(1);
        REnumeration baz = enums.get(2);

        assertEquals(Set.of(foo, bar, baz), allParents(baz));
        assertEquals(Set.of(foo, bar), allParents(bar));
        assertEquals(Set.of(foo), allParents(foo));

        GeneratorModel gm = new GeneratorModel(ws, m -> true);
        assertEquals(List.of("foo0", "foo1", "bar", "baz"),
                gm.allValues(baz).stream().map(v -> v.name()).toList());
        assertEquals(List.of("foo0", "foo1", "bar"),
                gm.allValues(bar).stream().map(v -> v.name()).toList());
        assertEquals(List.of("foo0", "foo1"),
                gm.allValues(foo).stream().map(v -> v.name()).toList());
    }
}
