package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.util.ModelGeneratorUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../util/ModelGeneratorUtilTest.xtend} — 5/5
 * methods. The fork's {@code ModelGeneratorUtil.javadoc} moved doc-ref
 * extraction to callers (the 4-arg form: definition, docRefs, version,
 * pojoStyle) — each port re-wires the upstream element-level call to
 * {@code javadoc(element.definition(), element.docReferences(), null, true)}.
 * ONE representation delta, documented here: upstream's helper returns the
 * block WITH a trailing newline (its template call sites end the
 * interpolation); the fork returns the block ENDING AT {@code *&#47;} and the
 * call sites append the newline — the composed emission is byte-locked by the
 * 34,686 corpus goldens, so the port asserts the fork form (the upstream
 * expected text minus the final newline; every other byte identical, trailing
 * spaces preserved via {@code \s}). Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamModelGeneratorUtilPortTest {

    private record Parsed(RWorkspace ws, RModel model) { }

    private static Parsed parse(String snippet) {
        RModel model = AstBuilder.buildFromString(
                UpstreamPortHarness.TEST_NS_HEADER + "\n" + snippet, "generator-util-port.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        return new Parsed(ws, model);
    }

    private static RDataType dataType(RModel model, String name) {
        for (var el : model.rootElements()) {
            if (el instanceof RDataType dt && name.equals(dt.name())) {
                return dt;
            }
        }
        throw new AssertionError("no type " + name + " in the parsed model");
    }

    /** Upstream {@code testDocReferenceJavaDoc}. */
    @Test
    void testDocReferenceJavaDoc() {
        Parsed p = parse("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                segment name

                type Foo:
                	[docReference Org1 Agr1 name "something" provision "some provision"]
                	bar string (1..1)
                """);
        RDataType foo = dataType(p.model(), "Foo");

        String javaDoc = new ModelGeneratorUtil(p.ws()).javadoc(
                foo.definition().orElse(null), foo.docReferences(), null, true);

        assertEquals("""
                /**
                 *
                 * Body Org1
                 * Corpus Agreement Agr1 Agreement 1 \s
                 * name "something"
                 *
                 * Provision some provision
                 *
                 */""",
                javaDoc);
    }

    /** Upstream {@code testMultiDocReferenceJavaDoc}. */
    @Test
    void testMultiDocReferenceJavaDoc() {
        Parsed p = parse("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                body Organisation Org2
                corpus View Org2 "View 2" Vw2

                segment name

                type Foo:
                	[docReference Org1 Agr1 name "something" provision "some provision"]
                	[docReference Org2 Vw2 name "something else" provision "some other provision"]

                	bar string (1..1)
                """);
        RDataType foo = dataType(p.model(), "Foo");

        String javaDoc = new ModelGeneratorUtil(p.ws()).javadoc(
                foo.definition().orElse(null), foo.docReferences(), null, true);

        assertEquals("""
                /**
                 *
                 * Body Org1
                 * Corpus Agreement Agr1 Agreement 1 \s
                 * name "something"
                 *
                 * Provision some provision
                 *
                 *
                 * Body Org2
                 * Corpus View Vw2 View 2 \s
                 * name "something else"
                 *
                 * Provision some other provision
                 *
                 */""",
                javaDoc);
    }

    /** Upstream {@code testDocReferenceAndDefJavaDoc}. */
    @Test
    void testDocReferenceAndDefJavaDoc() {
        Parsed p = parse("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                segment name

                type Foo: <"Foo def 12345">
                	[docReference Org1 Agr1 name "something" provision "some provision"]

                	bar string (1..1)
                """);
        RDataType foo = dataType(p.model(), "Foo");

        String javaDoc = new ModelGeneratorUtil(p.ws()).javadoc(
                foo.definition().orElse(null), foo.docReferences(), null, true);

        assertEquals("""
                /**
                 * Foo def 12345
                 *
                 * Body Org1
                 * Corpus Agreement Agr1 Agreement 1 \s
                 * name "something"
                 *
                 * Provision some provision
                 *
                 */""",
                javaDoc);
    }

    /** Upstream {@code testDefJavaDoc}. */
    @Test
    void testDefJavaDoc() {
        Parsed p = parse("""
                type Foo: <"Foo def 12345">
                	bar string (1..1)
                """);
        RDataType foo = dataType(p.model(), "Foo");

        String javaDoc = new ModelGeneratorUtil(p.ws()).javadoc(
                foo.definition().orElse(null), foo.docReferences(), null, true);

        assertEquals("""
                /**
                 * Foo def 12345
                 */""",
                javaDoc);
    }

    /** Upstream {@code testDocRefOnAttributeJavaDoc}. */
    @Test
    void testDocRefOnAttributeJavaDoc() {
        Parsed p = parse("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                body Organisation Org2
                corpus View Org2 "View 2" Vw2

                segment name

                type Foo:
                	bar string (1..1) <"Foo def 12345">
                	[docReference Org1 Agr1 name "something" provision "some provision"]
                	[docReference Org2 Vw2 name "something else" provision "some other provision"]

                """);
        RAttribute bar = dataType(p.model(), "Foo").attributes().stream()
                .filter(a -> "bar".equals(a.name())).findFirst().orElseThrow();

        String javaDoc = new ModelGeneratorUtil(p.ws()).javadoc(
                bar.definition().orElse(null), bar.docReferences(), null, true);

        assertEquals("""
                /**
                 * Foo def 12345
                 *
                 * Body Org1
                 * Corpus Agreement Agr1 Agreement 1 \s
                 * name "something"
                 *
                 * Provision some provision
                 *
                 *
                 * Body Org2
                 * Corpus View Vw2 View 2 \s
                 * name "something else"
                 *
                 * Provision some other provision
                 *
                 */""",
                javaDoc);
    }
}
