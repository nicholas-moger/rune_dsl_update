package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.util.AstVisitor;
import com.regnosys.rosetta.ast.util.AstWalker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AstWalker} and {@link AstVisitor} traversal
 * utilities (Task 15).
 *
 * <p>These tests use real parsed AST trees produced via {@link AstBuilder}
 * to verify that the walker and visitor correctly traverse, filter, and
 * dispatch over the typed AST.
 */
class AstTraversalTest extends BaseAstTest {

    // =========================================================================
    // AstWalker.walk — pre-order traversal
    // =========================================================================

    @Test
    void walk_visitsEveryNodeOnce() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)\n"
                + "    baz int (0..*)");
        List<RNode> visited = new ArrayList<>();
        AstWalker.walk(model, visited::add);

        // Pre-order: model first, then its children
        assertSame(model, visited.get(0));
        // Should include: model, dataType, 2 attributes, 2 typeCalls, 2 cardinalities (at least)
        assertTrue(visited.size() >= 7,
                "expected at least 7 nodes, got " + visited.size());
        // Each node visited at most once (use identity comparison)
        long uniqueCount = visited.stream().distinct().count();
        assertEquals(visited.size(), uniqueCount, "every node should be visited exactly once");
    }

    @Test
    void walk_throwsOnNullRoot() {
        assertThrows(NullPointerException.class,
                () -> AstWalker.walk(null, n -> {}));
    }

    @Test
    void walk_throwsOnNullConsumer() {
        RModel model = parseAndBuild("namespace test");
        assertThrows(NullPointerException.class,
                () -> AstWalker.walk(model, null));
    }

    // =========================================================================
    // AstWalker.findAll — type-filtered collection
    // =========================================================================

    @Test
    void findAll_returnsAllInstancesOfType() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)\n"
                + "type Bar:\n"
                + "    qux int (0..1)\n"
                + "type Baz:\n"
                + "    name string (1..1)");
        List<RDataType> dataTypes = AstWalker.findAll(model, RDataType.class);

        assertEquals(3, dataTypes.size());
        assertEquals("Foo", dataTypes.get(0).name());
        assertEquals("Bar", dataTypes.get(1).name());
        assertEquals("Baz", dataTypes.get(2).name());
    }

    @Test
    void findAll_returnsEmptyWhenNoneMatch() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)");
        List<REnumeration> enums = AstWalker.findAll(model, REnumeration.class);
        assertTrue(enums.isEmpty());
    }

    @Test
    void findAll_findsNestedAttributes() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    a string (1..1)\n"
                + "    b int (0..*)\n"
                + "type Bar:\n"
                + "    c boolean (1..1)");
        List<RAttribute> attrs = AstWalker.findAll(model, RAttribute.class);
        assertEquals(3, attrs.size());
    }

    @Test
    void findAll_predicate_filtersByPredicate() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    a string (1..1)\n"
                + "type Bar:\n"
                + "    b int (1..1)");
        List<RNode> namedFoo = AstWalker.findAll(model,
                node -> node instanceof RDataType dt && "Foo".equals(dt.name()));
        assertEquals(1, namedFoo.size());
        assertEquals("Foo", ((RDataType) namedFoo.get(0)).name());
    }

    // =========================================================================
    // AstWalker.findAncestor
    // =========================================================================

    @Test
    void findAncestor_findsEnclosingDataType() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)");
        RDataType dt = (RDataType) model.rootElements().get(0);
        RAttribute attr = dt.attributes().get(0);

        Optional<RDataType> ancestor = AstWalker.findAncestor(attr, RDataType.class);
        assertTrue(ancestor.isPresent());
        assertSame(dt, ancestor.get());
    }

    @Test
    void findAncestor_findsEnclosingModel() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)");
        RDataType dt = (RDataType) model.rootElements().get(0);
        RAttribute attr = dt.attributes().get(0);

        Optional<RModel> ancestor = AstWalker.findAncestor(attr, RModel.class);
        assertTrue(ancestor.isPresent());
        assertSame(model, ancestor.get());
    }

    @Test
    void findAncestor_returnsEmptyForRoot() {
        RModel model = parseAndBuild("namespace test");
        Optional<RNode> ancestor = AstWalker.findAncestor(model, RNode.class);
        assertTrue(ancestor.isEmpty(),
                "model has no parent, so any ancestor search should return empty");
    }

    @Test
    void findAncestor_doesNotMatchSelf() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)");
        RDataType dt = (RDataType) model.rootElements().get(0);

        // The data type itself is not its own ancestor
        Optional<RDataType> ancestor = AstWalker.findAncestor(dt, RDataType.class);
        assertTrue(ancestor.isEmpty());
    }

    // =========================================================================
    // AstWalker.findFirst
    // =========================================================================

    @Test
    void findFirst_returnsFirstMatchInPreOrder() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)\n"
                + "type Bar:\n"
                + "    baz int (1..1)");
        Optional<RDataType> first = AstWalker.findFirst(model, RDataType.class);
        assertTrue(first.isPresent());
        assertEquals("Foo", first.get().name());
    }

    @Test
    void findFirst_returnsEmptyWhenNoneMatch() {
        RModel model = parseAndBuild("namespace test");
        Optional<RDataType> first = AstWalker.findFirst(model, RDataType.class);
        assertTrue(first.isEmpty());
    }

    // =========================================================================
    // AstWalker.count
    // =========================================================================

    @Test
    void count_matchesFindAllSize() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    a string (1..1)\n"
                + "    b int (1..1)\n"
                + "type Bar:\n"
                + "    c boolean (1..1)");
        int count = AstWalker.count(model, RAttribute.class);
        assertEquals(3, count);
        assertEquals(AstWalker.findAll(model, RAttribute.class).size(), count);
    }

    // =========================================================================
    // AstVisitor — generic dispatch
    // =========================================================================

    @Test
    void astVisitor_dispatchesRootElementsToVisitRootElement() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)\n"
                + "enum Direction:\n"
                + "    North\n"
                + "    South");
        List<RRootElement> seen = new ArrayList<>();
        AstVisitor<Void> visitor = new AstVisitor<>() {
            @Override public Void visitRootElement(RRootElement node) {
                seen.add(node);
                return null;
            }
        };
        for (RRootElement re : model.rootElements()) {
            visitor.visit(re);
        }
        assertEquals(2, seen.size());
        assertInstanceOf(RDataType.class, seen.get(0));
        assertInstanceOf(REnumeration.class, seen.get(1));
    }

    @Test
    void astVisitor_visitDefaultIsFallback() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)");
        int[] defaultCount = {0};
        AstVisitor<Void> visitor = new AstVisitor<>() {
            @Override public Void visitDefault(RNode node) {
                defaultCount[0]++;
                return null;
            }
        };
        AstWalker.walk(model, visitor::visit);
        // Every node should hit visitDefault since no category methods are overridden
        assertTrue(defaultCount[0] > 0, "visitDefault should be called for every node");
    }

    @Test
    void astVisitor_patternMatchingDispatch() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)\n"
                + "func MyFunc:\n"
                + "    output:\n"
                + "        result string (1..1)");
        List<String> names = new ArrayList<>();
        AstVisitor<Void> namer = new AstVisitor<>() {
            @Override public Void visit(RNode node) {
                switch (node) {
                    case RDataType dt -> names.add("type:" + dt.name());
                    case RFunction fn -> names.add("func:" + fn.name());
                    default -> {}
                }
                return null;
            }
        };
        AstWalker.walk(model, namer::visit);
        assertEquals(2, names.size());
        assertEquals("type:Foo", names.get(0));
        assertEquals("func:MyFunc", names.get(1));
    }

    @Test
    void astVisitor_returnTypeIsThreadedThroughVisit() {
        RModel model = parseAndBuild(
                "namespace test\n"
                + "type Foo:\n"
                + "    bar string (1..1)");
        AstVisitor<String> renderer = new AstVisitor<>() {
            @Override public String visit(RNode node) {
                if (node instanceof RDataType dt) {
                    return "RENDERED:" + dt.name();
                }
                return null;
            }
        };
        RDataType dt = (RDataType) model.rootElements().get(0);
        assertEquals("RENDERED:Foo", renderer.visit(dt));
    }
}
