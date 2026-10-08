package com.regnosys.rosetta.ast.registry;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.util.AstInterfaceRegistry;
import com.regnosys.rosetta.ast.util.AstVisitor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstInterfaceRegistryTest {

    interface HasName { String name(); }
    interface HasShape { String shape(); }

    static final class NamedNode extends RNode implements HasName {
        private final String name;
        NamedNode(String name) { this.name = name; }
        @Override public List<? extends RNode> children() { return List.of(); }
        @Override public String name() { return name; }
    }

    static final class ShapedNode extends RNode implements HasShape {
        @Override public List<? extends RNode> children() { return List.of(); }
        @Override public String shape() { return "round"; }
    }

    static final class NamedAndShaped extends RNode implements HasName, HasShape {
        @Override public List<? extends RNode> children() { return List.of(); }
        @Override public String name() { return "x"; }
        @Override public String shape() { return "square"; }
    }

    @Test
    void register_singleInterface_dispatches() {
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<String>()
                .register(HasName.class, n -> n.name().toUpperCase());
        assertEquals("ALICE", reg.dispatch(new NamedNode("alice")));
    }

    @Test
    void register_multipleInterfaces_dispatchesByMatch() {
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<String>()
                .register(HasName.class, n -> "name:" + n.name())
                .register(HasShape.class, s -> "shape:" + s.shape());
        assertEquals("name:a", reg.dispatch(new NamedNode("a")));
        assertEquals("shape:round", reg.dispatch(new ShapedNode()));
    }

    @Test
    void dispatch_noHandler_returnsNull() {
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<String>()
                .register(HasName.class, HasName::name);
        assertNull(reg.dispatch(new ShapedNode()));
    }

    @Test
    void dispatch_multiInterfaceMatch_throwsIllegalStateException() {
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<String>()
                .register(HasName.class, HasName::name)
                .register(HasShape.class, HasShape::shape);
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> reg.dispatch(new NamedAndShaped()));
        assertTrue(ex.getMessage().contains("Multiple handlers match"));
        assertTrue(ex.getMessage().contains("HasName"));
        assertTrue(ex.getMessage().contains("HasShape"));
    }

    @Test
    void register_returnsThisForChaining() {
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<>();
        AstInterfaceRegistry<String> chained = reg.register(HasName.class, HasName::name);
        assertSame(reg, chained);
    }

    @Test
    void register_nullArgs_throws() {
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<>();
        assertThrows(NullPointerException.class,
                () -> reg.register(null, HasName::name));
        assertThrows(NullPointerException.class,
                () -> reg.register(HasName.class, null));
    }

    @Test
    void dispatch_nullNode_throws() {
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<>();
        assertThrows(NullPointerException.class, () -> reg.dispatch(null));
    }

    @Test
    void size_reflectsRegistrations() {
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<String>()
                .register(HasName.class, HasName::name)
                .register(HasShape.class, HasShape::shape);
        assertEquals(2, reg.size());
    }

    @Test
    void withInterfaceRegistry_categoryMethodsDelegateToBase() {
        // Regression test: the wrapper returned by withInterfaceRegistry()
        // must override EVERY category method, not just visit(RNode).
        // Otherwise a caller that invokes wrapped.visitNode(n) directly
        // would hit the interface default (visitDefault → null) instead
        // of the base visitor's overridden behaviour.
        AstVisitor<String> base = new AstVisitor<String>() {
            @Override public String visitBinaryExpression(RBinaryExpression n) { return "BIN"; }
            @Override public String visitExpression(RExpression n) { return "EXPR"; }
            @Override public String visitRootElement(RRootElement n) { return "ROOT"; }
            @Override public String visitNode(RNode n) { return "NODE"; }
            @Override public String visitDefault(RNode n) { return "DEFAULT"; }
        };
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<String>()
                .register(HasName.class, n -> "name:" + n.name());
        AstVisitor<String> wrapped = base.withInterfaceRegistry(reg);

        NamedNode named = new NamedNode("x");
        ShapedNode shaped = new ShapedNode();

        // visit() goes through the registry first
        assertEquals("name:x", wrapped.visit(named));
        // visit() falls back to base when registry has no match
        assertEquals("NODE", wrapped.visit(shaped));
        // Direct category-method calls delegate to base (the bug Copilot R8 caught)
        assertEquals("NODE", wrapped.visitNode(shaped));
        assertEquals("DEFAULT", wrapped.visitDefault(shaped));
    }

    @Test
    void register_duplicateIface_throwsIllegalStateException() {
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<String>()
                .register(HasName.class, HasName::name);
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> reg.register(HasName.class, n -> "other"));
        assertTrue(ex.getMessage().contains("Handler for HasName is already registered"));
    }

    @Test
    void dispatch_handlerReturnsNull_throws() {
        // Handlers MUST return non-null per the AstInterfaceRegistry contract.
        // Null is reserved for "no registered interface matches" — letting a
        // handler return null silently would make withInterfaceRegistry()
        // fall through to the base visitor instead of using the registry's
        // intended (null) result.
        AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<String>()
                .register(HasName.class, n -> null);
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> reg.dispatch(new NamedNode("a")));
        assertTrue(ex.getMessage().contains("Handler for HasName returned null"));
        assertTrue(ex.getMessage().contains("Optional<T>"));
    }
}
