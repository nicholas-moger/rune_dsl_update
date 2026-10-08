package com.regnosys.rosetta.generator.java.expression.coercers;

import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaReferenceWithMeta;
import com.rosetta.util.types.JavaReferenceType;
import com.rosetta.util.types.JavaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WrappedItemCoercerTest {

    private WrappedItemCoercer coercer;
    private JavaTypeUtil typeUtil;
    private TypeCoercionService service;
    private JavaStatementScope scope;

    @BeforeEach
    void setUp() {
        typeUtil = new JavaTypeUtil();
        service = new TypeCoercionService(typeUtil);
        coercer = new WrappedItemCoercer();
        scope = new JavaStatementScope("test", null);
    }

    // =========================================================================
    // MapperS<Integer> → MapperS<BigDecimal>
    // =========================================================================

    @Test
    void MapperS_Integer_to_MapperS_BigDecimal() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.BIG_DECIMAL);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        // Byte-faithful (upstream getMapperSItemConversionExpression + convertNullSafe):
        // <ExpectedItemType> witness + SCOPE-REGISTERED type-derived param (facet
        // meta_coercion_numbering — emitted as a deferred sentinel, resolved by
        // computeActualNames at finalization; render(...) applies the resolution)
        // + null-safe ternary.
        assertEquals("x.<BigDecimal>map(\"Type coercion\", integer -> integer == null ? null : BigDecimal.valueOf(integer))", render(result));
    }

    // =========================================================================
    // MapperC<Integer> → MapperC<BigDecimal>
    // =========================================================================

    @Test
    void MapperC_Integer_to_MapperC_BigDecimal() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.BIG_DECIMAL);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        // Byte-faithful (upstream getMapperCItemConversionExpression): <BigDecimal> witness
        // + disambiguated param, but NO null-safe ternary — a MapperC's items are non-null
        // (the 399 bare `…getValue()` goldens vs the 1,027 null-safe MapperS ones).
        assertEquals("x.<BigDecimal>map(\"Type coercion\", integer -> BigDecimal.valueOf(integer))", render(result));
    }

    // =========================================================================
    // List<Integer> → List<BigDecimal>
    // =========================================================================

    @Test
    void List_Integer_to_List_BigDecimal() {
        var actual = typeUtil.wrap(typeUtil.LIST, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.LIST, typeUtil.BIG_DECIMAL);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("x.stream().map(item -> BigDecimal.valueOf(item)).collect(Collectors.toList())", render(result));
    }

    // =========================================================================
    // Same item type returns unchanged
    // =========================================================================

    @Test
    void same_item_type_returns_unchanged() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertSame(expr, result);
    }

    // =========================================================================
    // MapperS<Long> → MapperS<Integer> (narrowing inside wrapper)
    // =========================================================================

    @Test
    void MapperS_Long_to_MapperS_Integer_throwOnFail_true() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.LONG);
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        // Byte-faithful: <Integer> witness + null-safe ternary + the `long` keyword
        // escaped to `_long` (the registered identifier's computeActualNames
        // invalid-identifier escape; same goldens apply to colliding params, e.g.
        // `_referenceWithMetaProductIdentifier`).
        assertEquals("x.<Integer>map(\"Type coercion\", _long -> _long == null ? null : Math.toIntExact(_long))", render(result));
    }

    // =========================================================================
    // MapperListOfLists<Integer> → MapperListOfLists<BigDecimal>
    // =========================================================================

    @Test
    void MapperListOfLists_Integer_to_MapperListOfLists_BigDecimal() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_LIST_OF_LISTS, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_LIST_OF_LISTS, typeUtil.BIG_DECIMAL);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("x.mapListToList(mapperC -> mapperC.map(\"Type coercion\", item -> BigDecimal.valueOf(item)))", render(result));
    }

    // =========================================================================
    // Meta-value unwrap inside a wrapper (the ~99% real coercion form):
    //   MapperS<ReferenceWithMetaParty> → MapperS<Party>
    //   MapperC<ReferenceWithMetaParty> → MapperC<Party>
    // The wrapper envelope is WrappedItemCoercer; the inner getValue() comes
    // from ItemToItemCoercer's meta-unwrap branch.
    // =========================================================================

    @Test
    void MapperS_meta_unwrap_ReferenceWithMetaParty_to_Party() {
        JavaReferenceType party = partyValueType();
        var meta = new RJavaReferenceWithMeta(party, metafieldsPackage(), typeUtil);
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, meta);
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, party);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        // MapperS → null-safe ternary body.
        assertEquals("x.<Party>map(\"Type coercion\", referenceWithMetaParty -> referenceWithMetaParty == null ? null : referenceWithMetaParty.getValue())", render(result));
    }

    @Test
    void MapperC_meta_unwrap_ReferenceWithMetaParty_to_Party() {
        JavaReferenceType party = partyValueType();
        var meta = new RJavaReferenceWithMeta(party, metafieldsPackage(), typeUtil);
        var actual = typeUtil.wrap(typeUtil.MAPPER_C, meta);
        var expected = typeUtil.wrap(typeUtil.MAPPER_C, party);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        // MapperC → bare body (items non-null).
        assertEquals("x.<Party>map(\"Type coercion\", referenceWithMetaParty -> referenceWithMetaParty.getValue())", render(result));
    }

    @Test
    void MapperS_meta_unwrap_registers_witness_import() {
        JavaReferenceType party = partyValueType();
        var meta = new RJavaReferenceWithMeta(party, metafieldsPackage(), typeUtil);
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, meta);
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, party);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        // The <Party> generic witness is rendered by simple name; its import must be
        // registered via the structured refs (upstream registers it implicitly via
        // JavaType interpolation). The identity getValue() body carries no Party ref,
        // so the witness type itself must be added — else <Party> emits unimported.
        assertTrue(refsOf(result).contains(party),
            "witness type Party must be in refs to import the <Party> generic");
    }

    /** A synthetic generated POJO named {@code Party} to act as a meta value type. */
    private JavaReferenceType partyValueType() {
        return (JavaReferenceType) RGeneratedJavaClass.createWithSuperclass(
                JavaPackageName.splitOnDotsAndEscape("com.rosetta.test"), "Party",
                typeUtil.ROSETTA_MODEL_OBJECT);
    }

    private JavaPackageName metafieldsPackage() {
        return JavaPackageName.splitOnDotsAndEscape("com.rosetta.test.metafields");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private String render(JavaStatementBuilder builder) {
        String rendered = builder instanceof JavaExpression e
                ? e.renderToString()
                : ((JavaExpression) builder.collapseToSingleExpression(scope)).renderToString();
        // facet meta_coercion_numbering: the guarded MapperS arm emits a deferred
        // sentinel for its registered param; resolve it the way the renderer's
        // finalizeDeferredNames does (no-op for the bare MapperC / List arms).
        return scope.resolveDeferredCoercionNames(rendered);
    }

    private java.util.Set<com.rosetta.util.types.JavaClass<?>> refsOf(JavaStatementBuilder builder) {
        JavaExpression e = (builder instanceof JavaExpression je) ? je
                : (JavaExpression) builder.collapseToSingleExpression(scope);
        return e.getRefs();
    }
}
