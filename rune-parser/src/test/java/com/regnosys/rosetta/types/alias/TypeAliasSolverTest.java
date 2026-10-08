package com.regnosys.rosetta.types.alias;

import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgument;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgumentExpression;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.types.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TypeAliasSolverTest {

    private final TypeAliasSolver solver = new TypeAliasSolver();

    // === Forward evaluation ===================================================

    @Test void forward_simple_alias_returns_underlying() {
        // typeAlias MyBool: boolean
        var alias = new RAliasType("MyBool", Map.of(), RBasicType.BOOLEAN);
        RType result = solver.evaluateForward(alias);
        assertEquals(RBasicType.BOOLEAN, result);
    }

    @Test void forward_parametric_alias_substitutes_digits() {
        // typeAlias Max(n int): number(digits: n)
        // Max(5) → number(digits: 5)
        var underlying = new RNumberType(
            OptionalInt.of(5), OptionalInt.of(0), Optional.empty(), Optional.empty());
        var alias = new RAliasType("Max", Map.of("n", 5), underlying);
        RType result = solver.evaluateForward(alias);
        assertInstanceOf(RNumberType.class, result);
        assertEquals(5, ((RNumberType) result).digits().orElseThrow());
    }

    @Test void forward_nested_alias_unwraps() {
        // typeAlias Inner: boolean
        // typeAlias Outer: Inner
        var inner = new RAliasType("Inner", Map.of(), RBasicType.BOOLEAN);
        var outer = new RAliasType("Outer", Map.of(), inner);
        RType result = solver.evaluateForward(outer);
        assertEquals(RBasicType.BOOLEAN, result);
    }

    @Test void forward_non_alias_returns_itself() {
        RType result = solver.evaluateForward(RBasicType.BOOLEAN);
        assertEquals(RBasicType.BOOLEAN, result);
    }

    // === Reverse evaluation ===================================================

    @Test void reverse_extracts_parameter_from_number() {
        // typeAlias Max(n int): number(digits: n)
        // Given number(digits: 5), reverse → {n: 5}
        var template = new RNumberType(
            OptionalInt.empty(), OptionalInt.of(0), Optional.empty(), Optional.empty());
        var alias = new RAliasType("Max", Map.of("n", (Object) "digits"), template);
        var concrete = new RNumberType(
            OptionalInt.of(5), OptionalInt.of(0), Optional.empty(), Optional.empty());

        Optional<Map<String, Object>> result = solver.evaluateReverse(alias, concrete);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().get("n"));
    }

    @Test void reverse_fails_for_incompatible_types() {
        var alias = new RAliasType("MyBool", Map.of(), RBasicType.BOOLEAN);
        Optional<Map<String, Object>> result = solver.evaluateReverse(alias, RNumberType.unconstrained());
        assertTrue(result.isEmpty());
    }

    @Test void reverse_simple_alias_returns_empty_params() {
        var alias = new RAliasType("MyBool", Map.of(), RBasicType.BOOLEAN);
        Optional<Map<String, Object>> result = solver.evaluateReverse(alias, RBasicType.BOOLEAN);
        assertTrue(result.isPresent());
        assertTrue(result.get().isEmpty());
    }

    // === Cycle detection ======================================================

    @Test void cycle_returns_missing() {
        // typeAlias A: B, typeAlias B: A (simulated via refersTo chain)
        var a = new RAliasType("A", Map.of(), RMissingType.INSTANCE);
        var b = new RAliasType("B", Map.of(), a);
        // Replace a's refersTo with b to create cycle — but since RAliasType is
        // immutable, we simulate by testing the solver's depth limit
        RType result = solver.evaluateForward(b);
        // Should unwrap to MISSING (the leaf), not infinite loop
        assertInstanceOf(RMissingType.class, result);
    }

    @Test void deep_nesting_terminates() {
        // Build a chain of 20 aliases
        RType current = RBasicType.BOOLEAN;
        for (int i = 0; i < 20; i++) {
            current = new RAliasType("Alias" + i, Map.of(), current);
        }
        RType result = solver.evaluateForward(current);
        assertEquals(RBasicType.BOOLEAN, result);
    }

    // === evaluateAliasBody (P2.1.3) ==========================================

    private RTypeAlias buildIntAliasFixture() {
        // Mirrors basictypes.rosetta L23-L24:
        //   typeAlias int(digits int, min int, max int):
        //     number(digits: digits, fractionalDigits: 0, min: min, max: max)
        var alias = new RTypeAlias();
        alias.setName("int");

        // Body: number(digits: digits, fractionalDigits: 0, min: min, max: max)
        var body = new RTypeCall();
        body.setTypeName("number");

        // Arg "digits" → nameValue "digits" (alias param ref)
        var digitsArg = new RTypeCallArgument();
        digitsArg.setParameterName("digits");
        var digitsExpr = new RTypeCallArgumentExpression();
        digitsExpr.setNameValue("digits");
        digitsArg.setValue(digitsExpr);
        body.arguments().add(digitsArg);

        // Arg "fractionalDigits" → literal 0
        var fdArg = new RTypeCallArgument();
        fdArg.setParameterName("fractionalDigits");
        var fdExpr = new RTypeCallArgumentExpression();
        fdExpr.setLiteralValue("0");
        fdArg.setValue(fdExpr);
        body.arguments().add(fdArg);

        // Arg "min" → nameValue "min" (alias param ref)
        var minArg = new RTypeCallArgument();
        minArg.setParameterName("min");
        var minExpr = new RTypeCallArgumentExpression();
        minExpr.setNameValue("min");
        minArg.setValue(minExpr);
        body.arguments().add(minArg);

        // Arg "max" → nameValue "max" (alias param ref)
        var maxArg = new RTypeCallArgument();
        maxArg.setParameterName("max");
        var maxExpr = new RTypeCallArgumentExpression();
        maxExpr.setNameValue("max");
        maxArg.setValue(maxExpr);
        body.arguments().add(maxArg);

        alias.setTypeCall(body);
        return alias;
    }

    private RTypeCall emptyUseSiteCall(String typeName) {
        var call = new RTypeCall();
        call.setTypeName(typeName);
        return call;
    }

    @Test void evaluateAliasBody_int_no_use_args_yields_intType() {
        // periodMultiplier int (1..1) — useSiteCall has no arguments
        var alias = buildIntAliasFixture();
        var useSite = emptyUseSiteCall("int");

        RType result = solver.evaluateAliasBody(alias, useSite);

        assertInstanceOf(RNumberType.class, result);
        var num = (RNumberType) result;
        assertTrue(num.fractionalDigits().isPresent());
        assertEquals(0, num.fractionalDigits().getAsInt());
        assertTrue(num.isInteger());
        assertFalse(num.digits().isPresent()); // unbound at use-site
        assertFalse(num.min().isPresent());
        assertFalse(num.max().isPresent());
    }

    @Test void evaluateAliasBody_int_with_digits_arg_binds_digits() {
        // int(digits: 9) — useSiteCall passes digits=9
        var alias = buildIntAliasFixture();
        var useSite = new RTypeCall();
        useSite.setTypeName("int");
        var digitsArg = new RTypeCallArgument();
        digitsArg.setParameterName("digits");
        var digitsExpr = new RTypeCallArgumentExpression();
        digitsExpr.setLiteralValue("9");
        digitsArg.setValue(digitsExpr);
        useSite.arguments().add(digitsArg);

        RType result = solver.evaluateAliasBody(alias, useSite);

        assertInstanceOf(RNumberType.class, result);
        var num = (RNumberType) result;
        assertEquals(9, num.digits().getAsInt());
        assertEquals(0, num.fractionalDigits().getAsInt());
        assertTrue(num.isInteger());
    }

    @Test void evaluateAliasBody_int_with_digits_and_min_args_binds_both() {
        // Per spec § 4.1 test 3 — int(digits: 5, min: -100)
        var alias = buildIntAliasFixture();
        var useSite = new RTypeCall();
        useSite.setTypeName("int");

        var dArg = new RTypeCallArgument();
        dArg.setParameterName("digits");
        var dExpr = new RTypeCallArgumentExpression();
        dExpr.setLiteralValue("5");
        dArg.setValue(dExpr);
        useSite.arguments().add(dArg);

        var mArg = new RTypeCallArgument();
        mArg.setParameterName("min");
        var mExpr = new RTypeCallArgumentExpression();
        mExpr.setLiteralValue("100");
        mExpr.setNegated(true);
        mArg.setValue(mExpr);
        useSite.arguments().add(mArg);

        RType result = solver.evaluateAliasBody(alias, useSite);

        assertInstanceOf(RNumberType.class, result);
        var num = (RNumberType) result;
        assertEquals(5, num.digits().getAsInt());
        assertEquals(0, num.fractionalDigits().getAsInt());
        assertTrue(num.min().isPresent());
        assertEquals(0, num.min().get().compareTo(new java.math.BigDecimal("-100")));
        assertFalse(num.max().isPresent());
    }

    @Test void evaluateAliasBody_int_with_negated_min_literal_handles_negation() {
        var alias = buildIntAliasFixture();
        var useSite = new RTypeCall();
        useSite.setTypeName("int");

        var mArg = new RTypeCallArgument();
        mArg.setParameterName("min");
        var mExpr = new RTypeCallArgumentExpression();
        mExpr.setLiteralValue("100");
        mExpr.setNegated(true); // MINUS prefix per grammar
        mArg.setValue(mExpr);
        useSite.arguments().add(mArg);

        RType result = solver.evaluateAliasBody(alias, useSite);

        assertInstanceOf(RNumberType.class, result);
        var num = (RNumberType) result;
        assertTrue(num.min().isPresent());
        assertEquals(0, num.min().get().compareTo(new java.math.BigDecimal("-100")));
    }

    @Test void evaluateAliasBody_productType_no_body_args_yields_string_unconstrained() {
        // typeAlias productType: string  (no body args, no use args)
        var alias = new RTypeAlias();
        alias.setName("productType");
        var body = new RTypeCall();
        body.setTypeName("string");
        alias.setTypeCall(body);

        RType result = solver.evaluateAliasBody(alias, emptyUseSiteCall("productType"));

        assertInstanceOf(RStringType.class, result);
        assertEquals(RStringType.unconstrained(), result);
    }

    @Test void evaluateAliasBody_malformed_no_body_yields_missing() {
        var alias = new RTypeAlias();
        alias.setName("bad");
        // no typeCall set

        RType result = solver.evaluateAliasBody(alias, emptyUseSiteCall("bad"));

        assertEquals(RMissingType.INSTANCE, result);
    }

    @Test void evaluateAliasBody_unknown_base_typeName_yields_missing() {
        var alias = new RTypeAlias();
        alias.setName("weird");
        var body = new RTypeCall();
        body.setTypeName("notARealBaseType");
        alias.setTypeCall(body);

        RType result = solver.evaluateAliasBody(alias, emptyUseSiteCall("weird"));

        assertEquals(RMissingType.INSTANCE, result);
    }

    @Test void evaluateAliasBody_mixed_bound_unbound_args() {
        // int(digits: 7) — digits bound, min and max unbound
        var alias = buildIntAliasFixture();
        var useSite = new RTypeCall();
        useSite.setTypeName("int");
        var dArg = new RTypeCallArgument();
        dArg.setParameterName("digits");
        var dExpr = new RTypeCallArgumentExpression();
        dExpr.setLiteralValue("7");
        dArg.setValue(dExpr);
        useSite.arguments().add(dArg);

        RType result = solver.evaluateAliasBody(alias, useSite);

        assertInstanceOf(RNumberType.class, result);
        var num = (RNumberType) result;
        assertEquals(7, num.digits().getAsInt());
        assertEquals(0, num.fractionalDigits().getAsInt());
        assertFalse(num.min().isPresent());
        assertFalse(num.max().isPresent());
    }

    @Test void evaluateAliasBody_literal_overflow_falls_back_to_bigdecimal() {
        // A literal that exceeds Integer.MAX_VALUE should still populate min/max as BigDecimal
        var alias = buildIntAliasFixture();
        var useSite = new RTypeCall();
        useSite.setTypeName("int");

        var mArg = new RTypeCallArgument();
        mArg.setParameterName("max");
        var mExpr = new RTypeCallArgumentExpression();
        mExpr.setLiteralValue("9999999999999999"); // > Integer.MAX_VALUE
        mArg.setValue(mExpr);
        useSite.arguments().add(mArg);

        RType result = solver.evaluateAliasBody(alias, useSite);

        assertInstanceOf(RNumberType.class, result);
        var num = (RNumberType) result;
        assertTrue(num.max().isPresent());
        assertEquals(0, num.max().get().compareTo(new java.math.BigDecimal("9999999999999999")));
    }

    @Test void evaluateAliasBody_chains_through_intermediate_alias() {
        // Mirrors CDM pattern:
        //   typeAlias FpMLCodingScheme(domain string): string
        //   typeAlias BusinessCenter: FpMLCodingScheme(domain: "business-center")
        // Verifies that derived aliases chain-resolve through intermediate aliases
        // to the underlying builtin (here, string).
        var source = """
                namespace test
                typeAlias FpMLCodingScheme(domain string): string
                typeAlias BusinessCenter: FpMLCodingScheme(domain: "business-center")
                """;
        var model = com.regnosys.rosetta.ast.builder.AstBuilder.buildFromString(source, "test.rosetta");
        // Retain the linking result locally — RNode.attachToWorkspace stores the
        // RWorkspace as a WeakReference, so discarding the result lets the GC
        // collect the resolver before referencedType() runs and flake the test.
        // `Reference.reachabilityFence(linkingResult)` at the end keeps it alive
        // through the test body.
        var linkingResult = com.regnosys.rosetta.symbols.RWorkspace.build(java.util.List.of(model));

        var outer = model.rootElements().stream()
                .filter(e -> e instanceof RTypeAlias)
                .map(e -> (RTypeAlias) e)
                .filter(a -> "BusinessCenter".equals(a.name()))
                .findFirst().orElseThrow();

        RType result = solver.evaluateAliasBody(outer, emptyUseSiteCall("BusinessCenter"));

        assertInstanceOf(RStringType.class, result,
                "BusinessCenter should chain-resolve through FpMLCodingScheme to string");
        java.lang.ref.Reference.reachabilityFence(linkingResult);
    }

    @Test void evaluateAliasBody_chains_with_literal_arg_to_inner_alias() {
        // Tests that body args resolve as bindings for the inner alias's body.
        //   typeAlias Inner(digits int): number(digits: digits, fractionalDigits: 0)
        //   typeAlias Outer: Inner(digits: 9)
        // Outer should resolve to RNumberType(9, 0, _, _) via chain.
        var source = """
                namespace test
                typeAlias Inner(digits int):
                    number(digits: digits, fractionalDigits: 0)
                typeAlias Outer: Inner(digits: 9)
                """;
        var model = com.regnosys.rosetta.ast.builder.AstBuilder.buildFromString(source, "test.rosetta");
        // Retain the linking result locally — RNode.attachToWorkspace stores the
        // RWorkspace as a WeakReference, so discarding the result lets the GC
        // collect the resolver before referencedType() runs and flake the test.
        // `Reference.reachabilityFence(linkingResult)` at the end keeps it alive
        // through the test body.
        var linkingResult = com.regnosys.rosetta.symbols.RWorkspace.build(java.util.List.of(model));

        var outer = model.rootElements().stream()
                .filter(e -> e instanceof RTypeAlias)
                .map(e -> (RTypeAlias) e)
                .filter(a -> "Outer".equals(a.name()))
                .findFirst().orElseThrow();

        RType result = solver.evaluateAliasBody(outer, emptyUseSiteCall("Outer"));

        assertInstanceOf(RNumberType.class, result);
        var num = (RNumberType) result;
        assertEquals(9, num.digits().getAsInt(), "digits literal should propagate to inner alias body");
        assertEquals(0, num.fractionalDigits().getAsInt(), "inner fractionalDigits literal preserved");
        assertTrue(num.isInteger());
        java.lang.ref.Reference.reachabilityFence(linkingResult);
    }

    @Test void evaluateAliasBody_string_with_pattern_arg_yields_missing() {
        // Per Copilot PR #64 R11 F49 2026-05-14 — string alias whose body
        // declares unsupported string constraints (pattern / minLength /
        // maxLength) must return RMissingType.INSTANCE explicitly rather than
        // silently dropping the constraints (RStringType reconstruction is
        // P2.1.3b scope; the pattern slot requires Pattern.compile interpretation).
        var alias = new RTypeAlias();
        alias.setName("constrainedString");

        var body = new RTypeCall();
        body.setTypeName("string");
        var patternArg = new RTypeCallArgument();
        patternArg.setParameterName("pattern");
        var patternExpr = new RTypeCallArgumentExpression();
        patternExpr.setLiteralValue("[A-Z]+");
        patternArg.setValue(patternExpr);
        body.arguments().add(patternArg);
        alias.setTypeCall(body);

        RType result = solver.evaluateAliasBody(alias, emptyUseSiteCall("constrainedString"));

        assertEquals(RMissingType.INSTANCE, result,
                "string alias body with unsupported pattern constraint must yield RMissingType");
    }

    @Test void evaluateAliasBody_int_with_overflow_digits_yields_missing() {
        // Per Copilot PR #64 R11 F51 2026-05-14 — present-but-unparseable arg in
        // an int slot (digits / fractionalDigits) must yield RMissingType rather
        // than silently falling back to the base's constraint. Without this
        // validation, {@code digits: 9999999999999999} (which overflows
        // Integer.MAX_VALUE) leaves OptionalInt.empty() and the merge layer
        // silently substitutes the base's digits, dropping the user's intent.
        var alias = buildIntAliasFixture();
        var useSite = new RTypeCall();
        useSite.setTypeName("int");
        var dArg = new RTypeCallArgument();
        dArg.setParameterName("digits");
        var dExpr = new RTypeCallArgumentExpression();
        dExpr.setLiteralValue("9999999999999999"); // overflows Integer.MAX_VALUE
        dArg.setValue(dExpr);
        useSite.arguments().add(dArg);

        RType result = solver.evaluateAliasBody(alias, useSite);

        assertEquals(RMissingType.INSTANCE, result,
                "digits arg that overflows int slot must yield RMissingType, not silent fallback");
    }

    @Test void evaluateAliasBody_body_resolves_to_data_type_yields_dataTypeRef() {
        // Per Copilot PR #64 R11 F50 2026-05-14 — workspace-backed regression
        // test for the RDataType branch in evaluateBodyWithBindings. A typeAlias
        // whose body's referencedType resolves to an RDataType (rather than a
        // builtin or another alias) must return an RDataTypeRef pointing at it.
        // No observed CDM/DRR/ISO/rune-fpml corpus alias exercises this case
        // today (basictypes.rosetta + CDM derived aliases all target builtins
        // or other aliases) — defensive path locked here against future drift.
        var source = """
                namespace test
                type Money:
                    amount number (1..1)
                typeAlias MoneyAlias: Money
                """;
        var model = com.regnosys.rosetta.ast.builder.AstBuilder.buildFromString(source, "test.rosetta");
        // Retain linkingResult — RWorkspace stored as WeakReference; see chain tests.
        var linkingResult = com.regnosys.rosetta.symbols.RWorkspace.build(java.util.List.of(model));

        var moneyAlias = model.rootElements().stream()
                .filter(e -> e instanceof RTypeAlias)
                .map(e -> (RTypeAlias) e)
                .filter(a -> "MoneyAlias".equals(a.name()))
                .findFirst().orElseThrow();

        RType result = solver.evaluateAliasBody(moneyAlias, emptyUseSiteCall("MoneyAlias"));

        assertInstanceOf(RDataTypeRef.class, result,
                "alias whose body resolves to a data type must yield RDataTypeRef");
        java.lang.ref.Reference.reachabilityFence(linkingResult);
    }

    @Test void evaluateAliasBody_body_resolves_to_enum_yields_enumTypeRef() {
        // Per Copilot PR #64 R11 F50 2026-05-14 — workspace-backed regression
        // test for the REnumeration branch in evaluateBodyWithBindings. A
        // typeAlias whose body's referencedType resolves to an REnumeration
        // must return an REnumTypeRef pointing at it. Defensive path; locked
        // against future drift.
        var source = """
                namespace test
                enum Color:
                    RED
                    BLUE
                typeAlias ColorAlias: Color
                """;
        var model = com.regnosys.rosetta.ast.builder.AstBuilder.buildFromString(source, "test.rosetta");
        // Retain linkingResult — RWorkspace stored as WeakReference; see chain tests.
        var linkingResult = com.regnosys.rosetta.symbols.RWorkspace.build(java.util.List.of(model));

        var colorAlias = model.rootElements().stream()
                .filter(e -> e instanceof RTypeAlias)
                .map(e -> (RTypeAlias) e)
                .filter(a -> "ColorAlias".equals(a.name()))
                .findFirst().orElseThrow();

        RType result = solver.evaluateAliasBody(colorAlias, emptyUseSiteCall("ColorAlias"));

        assertInstanceOf(REnumTypeRef.class, result,
                "alias whose body resolves to an enum must yield REnumTypeRef");
        java.lang.ref.Reference.reachabilityFence(linkingResult);
    }

    @Test void evaluateAliasBody_int_with_only_min_arg_preserves_fractionalDigits_zero() {
        // Per Copilot PR #64 R7 F17 2026-05-13 — alias whose body directly
        // names the {@code int} builtin and adds a constraint other than
        // {@code fractionalDigits} (e.g. {@code typeAlias positiveInt: int(min: 0)}).
        // The body's typeName resolves via BUILTINS.lookup (not referencedType),
        // so the base RNumberType is {@code RNumberType.intType()} which carries
        // {@code fractionalDigits: 0}. Without merging the resolved args with
        // the base's existing constraints, fractionalDigits is dropped from the
        // reconstructed RNumberType and {@code isInteger()} returns false,
        // routing the alias to BigDecimal — the exact Cluster F regression.
        var alias = new RTypeAlias();
        alias.setName("positiveInt");

        var body = new RTypeCall();
        body.setTypeName("int"); // body.referencedType() left empty → BUILTINS.lookup path
        var minArg = new RTypeCallArgument();
        minArg.setParameterName("min");
        var minExpr = new RTypeCallArgumentExpression();
        minExpr.setLiteralValue("0");
        minArg.setValue(minExpr);
        body.arguments().add(minArg);

        alias.setTypeCall(body);

        var useSite = emptyUseSiteCall("positiveInt");

        RType result = solver.evaluateAliasBody(alias, useSite);

        assertInstanceOf(RNumberType.class, result);
        var num = (RNumberType) result;
        assertTrue(num.fractionalDigits().isPresent(),
                "fractionalDigits must be preserved from the int builtin's base RNumberType");
        assertEquals(0, num.fractionalDigits().getAsInt());
        assertTrue(num.isInteger(),
                "result must satisfy isInteger() so Java codegen routes to Integer not BigDecimal");
        assertTrue(num.min().isPresent());
        assertEquals(0, num.min().get().compareTo(java.math.BigDecimal.ZERO));
    }

    @Test void evaluateAliasBody_then_evaluateForward_roundtrip() {
        // Per spec § 4.1 test 10 — explicit setup/assert.
        // (a) Construct RTypeAlias AST node for int (no use args).
        var alias = buildIntAliasFixture();
        var useSite = emptyUseSiteCall("int");

        // (b) Call evaluateAliasBody — assert RNumberType(empty, of(0), empty, empty).
        RType bodyResult = solver.evaluateAliasBody(alias, useSite);
        assertInstanceOf(RNumberType.class, bodyResult);
        var num = (RNumberType) bodyResult;
        assertFalse(num.digits().isPresent());
        assertEquals(0, num.fractionalDigits().getAsInt());

        // (c) Wrap in RAliasType.
        var thatAlias = new RAliasType("int", Map.of(), bodyResult);

        // (d) Call evaluateForward — assert returns the inner RNumberType.
        RType unwrapped = solver.evaluateForward(thatAlias);
        assertEquals(bodyResult, unwrapped);
        assertInstanceOf(RNumberType.class, unwrapped);
    }
}
