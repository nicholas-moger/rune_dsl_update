package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.enums.ConversionKind;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConversionHandlerTest {

    private ConversionHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new ConversionHandler();
        compiler = new ExpressionCompiler();
        ctx      = ExpressionContext.of(null, ExpressionCompilerTest.createTestScope());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String render(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        return ((JavaExpression) result).renderToString();
    }

    private RSymbolReference symbolRef(String name) {
        var ref = new RSymbolReference();
        ref.setName(name);
        return ref;
    }

    private RIntLiteral intLiteral(int value) {
        var lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    // =========================================================================
    // to-number
    // =========================================================================

    @Test
    void to_number_chains_checkedMap_BigDecimal() {
        var expr = new RConversionExpr();
        expr.setArgument(symbolRef("rawValue"));
        expr.setKind(ConversionKind.NUMBER);

        assertEquals(
            "MapperS.of(rawValue).checkedMap(\"to-number\", BigDecimal::new, NumberFormatException.class)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // to-int
    // =========================================================================

    @Test
    void to_int_chains_checkedMap_Integer_parseInt() {
        var expr = new RConversionExpr();
        expr.setArgument(symbolRef("numStr"));
        expr.setKind(ConversionKind.INT);

        assertEquals(
            "MapperS.of(numStr).checkedMap(\"to-int\", Integer::parseInt, NumberFormatException.class)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // to-string
    // =========================================================================

    @Test
    void to_string_chains_map_Object_toString() {
        // Untyped symbolRef → no source enum resolves, so the handler emits the
        // generic form (Object::toString). The label is the hyphenated operator
        // keyword "to-string" (golden carries ZERO camelCase "toString" labels
        // across all 5 cells; facet tostring_enum_source). The enum-source
        // SourceEnum::toDisplayString form is locked end-to-end by
        // FunctionToStringEnumSourceTest's whole-file anchors.
        var expr = new RToStringExpr();
        expr.setArgument(symbolRef("value"));

        assertEquals(
            "MapperS.of(value).map(\"to-string\", Object::toString)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // to-enum
    // =========================================================================

    @Test
    void to_enum_string_source_chains_checkedMap_fromDisplayName() {
        // Untyped symbolRef → compiled argument carries no expression type, so
        // the handler cannot prove an enum source and emits the string-source
        // form (EnumType::fromDisplayName). The description is the hyphenated
        // operator keyword "to-enum" (golden uses "to-enum" across all 5 cells;
        // the enum-source valueOf form is locked by the D11 corpus byte-oracle).
        var expr = new RConversionExpr();
        expr.setArgument(symbolRef("enumStr"));
        expr.setKind(ConversionKind.ENUM);
        expr.setTargetEnumName("CurrencyCodeEnum");

        assertEquals(
            "MapperS.of(enumStr).checkedMap(\"to-enum\", CurrencyCodeEnum::fromDisplayName, IllegalArgumentException.class)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // to-date
    // =========================================================================

    @Test
    void to_date_chains_checkedMap_Date_parse() {
        var expr = new RConversionExpr();
        expr.setArgument(symbolRef("dateStr"));
        expr.setKind(ConversionKind.DATE);

        assertEquals(
            "MapperS.of(dateStr).checkedMap(\"to-date\", Date::parse, DateTimeParseException.class)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // to-date-time
    // =========================================================================

    @Test
    void to_date_time_chains_checkedMap_LocalDateTime_parse() {
        var expr = new RConversionExpr();
        expr.setArgument(symbolRef("dtStr"));
        expr.setKind(ConversionKind.DATE_TIME);

        assertEquals(
            "MapperS.of(dtStr).checkedMap(\"to-date-time\", LocalDateTime::parse, DateTimeParseException.class)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // to-zoned-date-time
    // =========================================================================

    @Test
    void to_zoned_date_time_chains_checkedMap_ZonedDateTime_parse() {
        var expr = new RConversionExpr();
        expr.setArgument(symbolRef("zdtStr"));
        expr.setKind(ConversionKind.ZONED_DATE_TIME);

        assertEquals(
            "MapperS.of(zdtStr).checkedMap(\"to-zoned-date-time\", ZonedDateTime::parse, DateTimeParseException.class)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // to-time
    // =========================================================================

    @Test
    void to_time_chains_checkedMap_LocalTime_parse() {
        var expr = new RConversionExpr();
        expr.setArgument(symbolRef("timeStr"));
        expr.setKind(ConversionKind.TIME);

        // facet toTimeLambdaEscape (PR #419): the parse-lambda param routes through
        // the deferred lambda-param channel (upstream createUniqueIdentifier's
        // underscore-escape law — oracle golden expr-conversions-valid `_s` under a
        // shadowing input), so the render resolves through the finalization hook
        // exactly like the sibling handler tests (ReferenceHandlerTest /
        // NavigationHandlerTest); nothing shadows `s` in this unit scope → bare `s`.
        assertEquals(
            "MapperS.of(timeStr).checkedMap(\"to-time\", s -> LocalTime.parse(s, DateTimeFormatter.ISO_LOCAL_TIME), DateTimeParseException.class)",
            ctx.scope().resolveDeferredCoercionNames(
                    render(handler.handle(expr, ctx, compiler))));
    }

    // =========================================================================
    // Dispatch via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_conversion() {
        var expr = new RConversionExpr();
        expr.setArgument(intLiteral(5));
        expr.setKind(ConversionKind.NUMBER);

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "MapperS.of(5).checkedMap(\"to-number\", BigDecimal::new, NumberFormatException.class)",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_to_string() {
        var expr = new RToStringExpr();
        expr.setArgument(symbolRef("obj"));

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "MapperS.of(obj).map(\"to-string\", Object::toString)",
            ((JavaExpression) result).renderToString());
    }

    // =========================================================================
    // PR-A §9.1 C3a.4.c — refs-carrying invariant tests (D2 α + D7 η)
    // =========================================================================
    //
    // ConversionHandler L80 emits a checkedMap chain whose body varies by
    // ConversionKind. Each arm textually references distinct classes —
    // asserted here so the refs channel stays complete post-C3c.2 (which
    // deleted the BUILTIN_TYPE_FQN regex path; the structured refs channel
    // is now the sole feeder).

    private RConversionExpr conversion(ConversionKind kind) {
        var expr = new RConversionExpr();
        expr.setArgument(symbolRef("raw"));
        expr.setKind(kind);
        return expr;
    }

    @Test
    void to_number_emission_carries_big_decimal_ref() {
        var result = handler.handle(conversion(ConversionKind.NUMBER), ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.BIG_DECIMAL),
                "NUMBER arm emits BigDecimal::new — must carry BIG_DECIMAL ref");
    }

    @Test
    void to_time_emission_carries_local_time_formatter_and_parse_exception_refs() {
        var result = handler.handle(conversion(ConversionKind.TIME), ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.LOCAL_TIME),
                "TIME arm emits LocalTime.parse — must carry LOCAL_TIME ref");
        assertTrue(refs.contains(HandlerHelper.DATE_TIME_FORMATTER),
                "TIME arm emits DateTimeFormatter.ISO_LOCAL_TIME — must carry "
                        + "DATE_TIME_FORMATTER ref (D7 η: confirmed missing "
                        + "from the deleted FunctionGenerator.BUILTIN_TYPE_FQN "
                        + "pre-C3c.2; structured refs are now the sole feeder)");
        assertTrue(refs.contains(HandlerHelper.DATE_TIME_PARSE_EXCEPTION),
                "TIME arm emits DateTimeParseException.class — must carry "
                        + "DATE_TIME_PARSE_EXCEPTION ref (D7 η)");
    }

    @Test
    void to_date_emission_carries_rosetta_date_and_parse_exception_refs() {
        var result = handler.handle(conversion(ConversionKind.DATE), ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.ROSETTA_DATE),
                "DATE arm emits Date::parse — must carry ROSETTA_DATE ref "
                        + "(com.rosetta.model.lib.records.Date — D2 α)");
        assertTrue(refs.contains(HandlerHelper.DATE_TIME_PARSE_EXCEPTION),
                "DATE arm emits DateTimeParseException.class — D7 η");
    }

    @Test
    void to_date_time_emission_carries_local_date_time_and_parse_exception_refs() {
        var result = handler.handle(conversion(ConversionKind.DATE_TIME), ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.LOCAL_DATE_TIME),
                "DATE_TIME arm emits LocalDateTime::parse — must carry "
                        + "LOCAL_DATE_TIME ref (D2 α)");
        assertTrue(refs.contains(HandlerHelper.DATE_TIME_PARSE_EXCEPTION),
                "DATE_TIME arm emits DateTimeParseException.class — D7 η");
    }

    @Test
    void to_zoned_date_time_emission_carries_zoned_and_parse_exception_refs() {
        var result = handler.handle(conversion(ConversionKind.ZONED_DATE_TIME), ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.ZONED_DATE_TIME),
                "ZONED_DATE_TIME arm emits ZonedDateTime::parse — must carry "
                        + "ZONED_DATE_TIME ref (D2 α)");
        assertTrue(refs.contains(HandlerHelper.DATE_TIME_PARSE_EXCEPTION),
                "ZONED_DATE_TIME arm emits DateTimeParseException.class — D7 η");
    }

    @Test
    void to_int_emission_carries_no_library_refs() {
        // INT arm emits Integer::parseInt + NumberFormatException.class —
        // both java.lang, no refs needed.
        var result = handler.handle(conversion(ConversionKind.INT), ctx, compiler);
        Set<?> refs = result.getRefs();
        assertFalse(refs.contains(HandlerHelper.BIG_DECIMAL),
                "INT arm is java.lang only — no BIG_DECIMAL ref");
        assertFalse(refs.contains(HandlerHelper.LOCAL_TIME),
                "INT arm is java.lang only — no date-time refs");
    }

    @Test
    void to_string_emission_carries_no_library_refs() {
        // Untyped source → the generic Object::toString form, which is java.lang
        // only. (An ENUM source additionally registers the source enum's import
        // ref — locked by FunctionToStringEnumSourceTest's whole-file anchors.)
        var expr = new RToStringExpr();
        expr.setArgument(symbolRef("obj"));
        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        assertFalse(refs.contains(HandlerHelper.BIG_DECIMAL),
                "an untyped to-string source uses Object::toString — no library refs");
    }

    // =========================================================================
    // to-enum source discrimination (the enum-source vs string-source branch)
    // =========================================================================
    //
    // The end-to-end enum-source paths are locked elsewhere: the inferred-type
    // step (a real enum-typed argument inferred from the workspace -> the
    // valueOf form emitted) by the D11 corpus byte-oracle (7 drr rule POJOs),
    // and the navigation-source fallback (NavigationHandler.leafEnumeration on
    // a disguised-REnumValueRef / chained-RFeatureCall source, + the target-enum
    // import ref) by FunctionCheckedMapEnumSourceTest's whole-file anchors
    // through the real D11 loader. A fully-wired ExpressionCompiler unit test
    // is impractical here (RWorkspace is final + the inference engine needs a
    // parsed model + no Mockito on the classpath), so the two pure seams the
    // ENUM arm composes are unit-tested directly.

    private static REnumeration enumeration(String name) {
        var en = new REnumeration();
        en.setName(name);
        return en;
    }

    @Test
    void isEnumSourceType_true_for_enum_type() {
        var inferred = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(enumeration("CurrencyCodeEnum")));
        assertTrue(ConversionHandler.isEnumSourceType(inferred),
                "an REnumTypeRef source must select the enum (valueOf) form");
    }

    @Test
    void isEnumSourceType_false_for_non_enum_type() {
        var inferred = RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
        assertFalse(ConversionHandler.isEnumSourceType(inferred),
                "a non-enum (e.g. string/basic) source must select the fromDisplayName form");
    }

    @Test
    void isEnumSourceType_false_for_missing_or_null() {
        assertFalse(ConversionHandler.isEnumSourceType(RMetaAnnotatedType.MISSING),
                "an un-inferred (MISSING) type falls back to the string-source form");
        assertFalse(ConversionHandler.isEnumSourceType(null),
                "a null inferred type falls back to the string-source form");
    }

    @Test
    void enumConversionFunction_enum_source_uses_valueOf_by_constant_name() {
        assertEquals("e -> CurrencyCodeEnum.valueOf(e.name())",
                ConversionHandler.enumConversionFunction("CurrencyCodeEnum", true));
    }

    @Test
    void enumConversionFunction_string_source_uses_fromDisplayName() {
        assertEquals("CurrencyCodeEnum::fromDisplayName",
                ConversionHandler.enumConversionFunction("CurrencyCodeEnum", false));
    }
}
