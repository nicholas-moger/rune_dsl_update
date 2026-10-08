package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.expressions.supporting.RWithMetaEntry;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.regnosys.rosetta.generator.java.SilentDegradation;

class ConstructionHandlerTest {

    /**
     * seat-21 review (MF-2, LAW 69): the with-meta constructor names its setters through THE ONE
     * meta-face → POJO property-name table (HandlerHelper.metaPojoPropertyName) — the complete mirror
     * this handler carried is gone; every face maps as upstream PojoPropertyUtil.toPojoPropertyName.
     */
    @Test
    void metaSetterName_namesEveryFaceThroughTheOneTable() {
        assertEquals("setExternalKey", ConstructionHandler.metaSetterName(withMetaEntry("key")));
        assertEquals("setExternalKey", ConstructionHandler.metaSetterName(withMetaEntry("id")));
        assertEquals("setExternalReference", ConstructionHandler.metaSetterName(withMetaEntry("reference")));
        assertEquals("setReference", ConstructionHandler.metaSetterName(withMetaEntry("address")));
        assertEquals("setScopedKey", ConstructionHandler.metaSetterName(withMetaEntry("location")));
        assertEquals("setScheme", ConstructionHandler.metaSetterName(withMetaEntry("scheme")));
        assertEquals("setTemplate", ConstructionHandler.metaSetterName(withMetaEntry("template")));
    }

    /**
     * seat-21 review (MF-2, v3.1 C0): an entry the parser left UNNAMED is refused loudly — the table's
     * null passthrough would otherwise render the literal `.setnull(…)` into generated Java (today's
     * catch-alls rethrow only a Refusal, so any other throw would have been swallowed into a TODO stub).
     */
    @Test
    void metaSetterName_refusesAnUnnamedEntryLoudly() {
        RWithMetaEntry unnamed = new RWithMetaEntry();
        SilentDegradation.Refusal refusal = assertThrows(SilentDegradation.Refusal.class,
                () -> ConstructionHandler.metaSetterName(unnamed));
        assertTrue(refusal.getMessage().contains("WITH_META_ENTRY_UNNAMED"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("setnull"), refusal.getMessage());
    }

    private static RWithMetaEntry withMetaEntry(String metaName) {
        RWithMetaEntry entry = new RWithMetaEntry();
        entry.setMetaName(metaName);
        return entry;
    }

    private ConstructionHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new ConstructionHandler();
        compiler = new ExpressionCompiler();
        ctx      = ExpressionContext.of(null, ExpressionCompilerTest.createTestScope());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private String render(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        return ((JavaExpression) result).renderToString();
    }

    private RStringLiteral strLit(String value) {
        var lit = new RStringLiteral();
        lit.setValue(value);
        return lit;
    }

    private RIntLiteral intLit(int value) {
        var lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    private RSymbolReference symbolRef(String name) {
        var ref = new RSymbolReference();
        ref.setName(name);
        return ref;
    }

    private RKeyValuePair kvPair(String key, com.regnosys.rosetta.ast.RExpression value) {
        var pair = new RKeyValuePair();
        pair.setKey(key);
        pair.setValue(value);
        return pair;
    }

    // =========================================================================
    // Constructor with key-value pairs
    // =========================================================================

    @Test
    void constructor_with_single_pair_generates_builder_chain() {
        var typeCall = new RTypeCall();
        typeCall.setTypeName("Party");

        var expr = new RConstructorExpr();
        expr.setTypeCall(typeCall);
        expr.pairs().add(kvPair("name", strLit("ACME")));

        assertEquals(
            "Party.builder().setName(MapperS.of(\"ACME\")).build()",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void constructor_with_multiple_pairs_generates_chained_setters() {
        var typeCall = new RTypeCall();
        typeCall.setTypeName("TradeState");

        var expr = new RConstructorExpr();
        expr.setTypeCall(typeCall);
        expr.pairs().add(kvPair("trade", symbolRef("myTrade")));
        expr.pairs().add(kvPair("quantity", intLit(100)));

        assertEquals(
            "TradeState.builder().setTrade(MapperS.of(myTrade)).setQuantity(MapperS.of(100)).build()",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void constructor_with_no_pairs_generates_empty_builder() {
        var typeCall = new RTypeCall();
        typeCall.setTypeName("Empty");

        var expr = new RConstructorExpr();
        expr.setTypeCall(typeCall);

        assertEquals(
            "Empty.builder().build()",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void constructor_with_null_type_call_uses_unknown() {
        var expr = new RConstructorExpr();
        // no typeCall set
        expr.pairs().add(kvPair("x", intLit(1)));

        assertEquals(
            "Unknown.builder().setX(MapperS.of(1)).build()",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // With-meta expression
    // =========================================================================

    // v3.2 seat 13 (D53, site R5 WITH_META_UNTYPED_STUB, commit 4): the stateless unit compiler has no type
    // information, so every with-meta construction here reached the legacy M7b-1 stub
    // (`MapperS.of(x).toBuilder().setMeta(MetaFields.builder()...).build()` - non-compiling Java no golden carries,
    // documented so since PR #202); the stub is REFUSED since seat 13 and these three pins now assert the refusal.
    // The typed arms (the annotated-output wrapper, the POJO's own MetaFields, the expression-derived wrapper) are
    // witnessed by the hold-out oracle groups (withmeta-wrapped-argument and its siblings) and the closing seat suite's n6.
    @Test
    void with_meta_generates_meta_fields_builder() {
        var entry = new RWithMetaEntry();
        entry.setMetaName("scheme");
        entry.setValue(strLit("http://fpml.org"));

        var expr = new RWithMetaExpr();
        expr.setArgument(symbolRef("currency"));
        expr.entries().add(entry);

        SilentDegradation.Refusal refusal = assertThrows(SilentDegradation.Refusal.class,
                () -> handler.handle(expr, ctx, compiler));
        assertTrue(refusal.getMessage().contains("WITH_META_UNTYPED_STUB"), refusal.getMessage());
    }

    @Test
    void with_meta_multiple_entries_chains_all_setters() {
        var entry1 = new RWithMetaEntry();
        entry1.setMetaName("scheme");
        entry1.setValue(strLit("http://scheme"));

        var entry2 = new RWithMetaEntry();
        entry2.setMetaName("key");
        entry2.setValue(strLit("abc-123"));

        var expr = new RWithMetaExpr();
        expr.setArgument(symbolRef("field"));
        expr.entries().add(entry1);
        expr.entries().add(entry2);

        SilentDegradation.Refusal refusal = assertThrows(SilentDegradation.Refusal.class,
                () -> handler.handle(expr, ctx, compiler));
        assertTrue(refusal.getMessage().contains("WITH_META_UNTYPED_STUB"), refusal.getMessage());
    }

    // =========================================================================
    // Dispatch via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_constructor() {
        var typeCall = new RTypeCall();
        typeCall.setTypeName("Price");

        var expr = new RConstructorExpr();
        expr.setTypeCall(typeCall);
        expr.pairs().add(kvPair("amount", intLit(50)));

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "Price.builder().setAmount(MapperS.of(50)).build()",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_with_meta() {
        var entry = new RWithMetaEntry();
        entry.setMetaName("scheme");
        entry.setValue(strLit("http://test"));

        var expr = new RWithMetaExpr();
        expr.setArgument(symbolRef("val"));
        expr.entries().add(entry);

        var scope  = ExpressionCompilerTest.createTestScope();
        SilentDegradation.Refusal refusal = assertThrows(SilentDegradation.Refusal.class,
                () -> compiler.compile(expr, null, scope));
        assertTrue(refusal.getMessage().contains("WITH_META_UNTYPED_STUB"), refusal.getMessage());
    }
}
