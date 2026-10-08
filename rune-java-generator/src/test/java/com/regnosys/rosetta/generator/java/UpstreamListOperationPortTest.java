package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #427, slice 3b-i) of upstream
 * {@code rune-integration-tests/.../expression/ListOperationTest.xtend} —
 * 71 upstream methods = 69 active + 2 upstream-{@code @Disabled} carried.
 * The split (probe-measured at #427; re-measured after the #429/#430/#431
 * heal waves): <b>69 GREEN-as-upstream — ZERO PINS</b> (15 full-text locks
 * byte-identical to the upstream expected blocks + 3 text+invoke hybrids
 * with byte-identical locks + 47 invoke batteries through the released
 * runtime + the 4-method namespace TEXT quartet).
 * Findings <b>#16</b> (closureParamTypingChannel — all 12 pins) and
 * <b>#19</b> (thenCountTerminalCoercion — the FilterListAndCount pin) were
 * HEALED at the #429 typing-channel wave; finding <b>#17</b>
 * (listOfListLowering — all 4 residual LOL arms: the named-closure-param
 * .mapItemToList lowering, the filter-over-LOL filterListNullSafe with the
 * LOL-preserving decl, and the LOL-lambda raw-param law) and the
 * <b>#20</b> ListOp face (ifArmMultiCoercion — the if-arm .getMulti()
 * coercion) were HEALED at the #430 wave; finding <b>#18</b>
 * (mapperCThenInvention — the last 3 pins: MultipleFilterList's
 * elseful-boolean filter-predicate chain admission, MultipleFilterList2's
 * implicit twin, and NestedFilters' CR-terminal filter-predicate chain)
 * was HEALED at the #431 then-hoist wave (the then-chains now hoist
 * thenArg locals / in-lambda block lambdas instead of rendering the
 * nonexistent MapperC.then(...)). All healed methods now run upstream's
 * own asserts; seven of them (NestedExtracts, NestedMaps2, ReduceString,
 * ReduceComplexType at #429; ExtractListOfListThenExtractToListOfCounts,
 * ExtractListOfListsThenFlatten, ListWithinIf at #430) additionally lock
 * the upstream expected text byte-identically.
 *
 * <p>Port deltas (semantics-identical): hamcrest {@code hasItems} →
 * {@code List.containsAll}/{@code contains}; Guava {@code ImmutableList.of}
 * → {@code List.of}; construction and invocation fully reflective (the
 * isolated-loader law — {@code RosettaModelObject} results held as
 * {@code Object}); upstream's unused {@code fooList} locals in the
 * sort/reverse methods dropped (the invoked input is the inline list).
 * Models and expected blocks extracted MECHANICALLY from the upstream xtend
 * (the banked {@code target/425-expected/} pipeline).
 * Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamListOperationPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListItemParameter}. */
    @Test
    void shouldGenerateFunctionWithFilterListItemParameter() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		filteredFoos Foo (0..*)
                \t
                	set filteredFoos:
                		foos\s
                			filter [ item -> include = True ]
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param foos\s
                	* @return filteredFoos\s
                	*/
                	public List<? extends Foo> evaluate(List<? extends Foo> foos) {
                		List<Foo.FooBuilder> filteredFoosBuilder = doEvaluate(foos);
                \t\t
                		final List<? extends Foo> filteredFoos;
                		if (filteredFoosBuilder == null) {
                			filteredFoos = null;
                		} else {
                			filteredFoos = filteredFoosBuilder.stream().map(Foo::build).collect(Collectors.toList());
                			objectValidator.validate(Foo.class, filteredFoos);
                		}
                \t\t
                		return filteredFoos;
                	}
                
                	protected abstract List<Foo.FooBuilder> doEvaluate(List<? extends Foo> foos);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Foo.FooBuilder> doEvaluate(List<? extends Foo> foos) {
                			if (foos == null) {
                				foos = Collections.emptyList();
                			}
                			List<Foo.FooBuilder> filteredFoos = new ArrayList<>();
                			return assignOutput(filteredFoos, foos);
                		}
                \t\t
                		protected List<Foo.FooBuilder> assignOutput(List<Foo.FooBuilder> filteredFoos, List<? extends Foo> foos) {
                			filteredFoos = toBuilder(MapperC.<Foo>of(foos)
                				.filterItemNullSafe(item -> areEqual(item.<Boolean>map("getInclude", foo -> foo.getInclude()), MapperS.of(true), CardinalityOperator.All).get()).getMulti());
                \t\t\t
                			return Optional.ofNullable(filteredFoos)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "a");
        Object foo2 = foo(classes, true, "b");
        Object foo3 = foo(classes, false, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2)), "hasItems(foo1, foo2)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListNamedParameter}. */
    @Test
    void shouldGenerateFunctionWithFilterListNamedParameter() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		filteredFoos Foo (0..*)
                \t
                	set filteredFoos:
                		foos\s
                			filter fooItem [ fooItem -> include = True ]
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param foos\s
                	* @return filteredFoos\s
                	*/
                	public List<? extends Foo> evaluate(List<? extends Foo> foos) {
                		List<Foo.FooBuilder> filteredFoosBuilder = doEvaluate(foos);
                \t\t
                		final List<? extends Foo> filteredFoos;
                		if (filteredFoosBuilder == null) {
                			filteredFoos = null;
                		} else {
                			filteredFoos = filteredFoosBuilder.stream().map(Foo::build).collect(Collectors.toList());
                			objectValidator.validate(Foo.class, filteredFoos);
                		}
                \t\t
                		return filteredFoos;
                	}
                
                	protected abstract List<Foo.FooBuilder> doEvaluate(List<? extends Foo> foos);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Foo.FooBuilder> doEvaluate(List<? extends Foo> foos) {
                			if (foos == null) {
                				foos = Collections.emptyList();
                			}
                			List<Foo.FooBuilder> filteredFoos = new ArrayList<>();
                			return assignOutput(filteredFoos, foos);
                		}
                \t\t
                		protected List<Foo.FooBuilder> assignOutput(List<Foo.FooBuilder> filteredFoos, List<? extends Foo> foos) {
                			filteredFoos = toBuilder(MapperC.<Foo>of(foos)
                				.filterItemNullSafe(fooItem -> areEqual(fooItem.<Boolean>map("getInclude", foo -> foo.getInclude()), MapperS.of(true), CardinalityOperator.All).get()).getMulti());
                \t\t\t
                			return Optional.ofNullable(filteredFoos)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "a");
        Object foo2 = foo(classes, true, "b");
        Object foo3 = foo(classes, false, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2)), "hasItems(foo1, foo2)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterList2}. */
    @Test
    void shouldGenerateFunctionWithFilterList2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo2:
                	include boolean (1..1)
                	include2 boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo2 (0..*)
                	output:
                		filteredFoos Foo2 (0..*)
                \t
                	set filteredFoos:
                		foos\s
                			filter [ item -> include = True and item -> include2 = True ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo2(classes, true, true, "a");
        Object foo2 = foo2(classes, true, false, "b");
        Object foo3 = foo2(classes, true, false, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(1, res.size());
        assertTrue(res.contains(foo1), "hasItems(foo1)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterList3}. */
    @Test
    void shouldGenerateFunctionWithFilterList3() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo2:
                	include boolean (1..1)
                	include2 boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo2 (0..*)
                	output:
                		filteredFoos Foo2 (0..*)
                \t
                	set filteredFoos:
                		foos\s
                			filter [ item -> include = True ]
                			then filter [ item -> include2 = True ]
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Foo2;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param foos\s
                	* @return filteredFoos\s
                	*/
                	public List<? extends Foo2> evaluate(List<? extends Foo2> foos) {
                		List<Foo2.Foo2Builder> filteredFoosBuilder = doEvaluate(foos);
                \t\t
                		final List<? extends Foo2> filteredFoos;
                		if (filteredFoosBuilder == null) {
                			filteredFoos = null;
                		} else {
                			filteredFoos = filteredFoosBuilder.stream().map(Foo2::build).collect(Collectors.toList());
                			objectValidator.validate(Foo2.class, filteredFoos);
                		}
                \t\t
                		return filteredFoos;
                	}
                
                	protected abstract List<Foo2.Foo2Builder> doEvaluate(List<? extends Foo2> foos);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Foo2.Foo2Builder> doEvaluate(List<? extends Foo2> foos) {
                			if (foos == null) {
                				foos = Collections.emptyList();
                			}
                			List<Foo2.Foo2Builder> filteredFoos = new ArrayList<>();
                			return assignOutput(filteredFoos, foos);
                		}
                \t\t
                		protected List<Foo2.Foo2Builder> assignOutput(List<Foo2.Foo2Builder> filteredFoos, List<? extends Foo2> foos) {
                			final MapperC<Foo2> thenArg = MapperC.<Foo2>of(foos)
                				.filterItemNullSafe(item -> areEqual(item.<Boolean>map("getInclude", foo2 -> foo2.getInclude()), MapperS.of(true), CardinalityOperator.All).get());
                			filteredFoos = toBuilder(thenArg
                				.filterItemNullSafe(item -> areEqual(item.<Boolean>map("getInclude2", foo2 -> foo2.getInclude2()), MapperS.of(true), CardinalityOperator.All).get()).getMulti());
                \t\t\t
                			return Optional.ofNullable(filteredFoos)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo2(classes, true, true, "a");
        Object foo2 = foo2(classes, true, false, "b");
        Object foo3 = foo2(classes, true, false, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(1, res.size());
        assertTrue(res.contains(foo1), "hasItems(foo1)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListWithMetaData}. */
    @Test
    void shouldGenerateFunctionWithFilterListWithMetaData() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type FooWithScheme:
                	attr string (1..1)
                		[metadata scheme]
                
                func FuncFoo:
                 	inputs:
                 		foos FooWithScheme (0..*)
                	output:
                		filteredFoos FooWithScheme (0..*)
                \t
                	set filteredFoos:
                		foos\s
                			filter [ item -> attr -> scheme = "foo-scheme" ]
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.model.metafields.FieldWithMetaString;
                import com.rosetta.test.model.FooWithScheme;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param foos\s
                	* @return filteredFoos\s
                	*/
                	public List<? extends FooWithScheme> evaluate(List<? extends FooWithScheme> foos) {
                		List<FooWithScheme.FooWithSchemeBuilder> filteredFoosBuilder = doEvaluate(foos);
                \t\t
                		final List<? extends FooWithScheme> filteredFoos;
                		if (filteredFoosBuilder == null) {
                			filteredFoos = null;
                		} else {
                			filteredFoos = filteredFoosBuilder.stream().map(FooWithScheme::build).collect(Collectors.toList());
                			objectValidator.validate(FooWithScheme.class, filteredFoos);
                		}
                \t\t
                		return filteredFoos;
                	}
                
                	protected abstract List<FooWithScheme.FooWithSchemeBuilder> doEvaluate(List<? extends FooWithScheme> foos);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<FooWithScheme.FooWithSchemeBuilder> doEvaluate(List<? extends FooWithScheme> foos) {
                			if (foos == null) {
                				foos = Collections.emptyList();
                			}
                			List<FooWithScheme.FooWithSchemeBuilder> filteredFoos = new ArrayList<>();
                			return assignOutput(filteredFoos, foos);
                		}
                \t\t
                		protected List<FooWithScheme.FooWithSchemeBuilder> assignOutput(List<FooWithScheme.FooWithSchemeBuilder> filteredFoos, List<? extends FooWithScheme> foos) {
                			filteredFoos = toBuilder(MapperC.<FooWithScheme>of(foos)
                				.filterItemNullSafe(item -> areEqual(item.<FieldWithMetaString>map("getAttr", fooWithScheme -> fooWithScheme.getAttr()).map("getMeta", a->a.getMeta()).map("getScheme", a->a.getScheme()), MapperS.of("foo-scheme"), CardinalityOperator.All).get()).getMulti());
                \t\t\t
                			return Optional.ofNullable(filteredFoos)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = fooWithScheme(classes, "a", "foo-scheme");
        Object foo2 = fooWithScheme(classes, "b", "foo-scheme");
        Object foo3 = fooWithScheme(classes, "c", "bar-scheme");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2)), "hasItems(foo1, foo2)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListWithMetaData2} — upstream-@Disabled, carried (generation-only body; never runs). */
    @Test
    @Disabled("upstream-@Disabled; the fork additionally throws at GENERATION on this [metadata scheme]-filter shape (probe-measured GEN_THROW)")
    void shouldGenerateFunctionWithFilterListWithMetaData2() {
        UpstreamPortHarness.generateCode("""
                type FooWithScheme:
                	attr string (1..1)
                		[metadata scheme]
                
                func FuncFoo:
                 	inputs:
                 		foos FooWithScheme (0..*)
                	output:
                		strings string (0..*)
                \t
                	set strings:
                		foos\s
                			map [ item -> attr ]
                			filter [ item -> scheme = "foo-scheme" ]
                """);
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterBuiltInTypeList}. */
    @Test
    void shouldGenerateFunctionWithFilterBuiltInTypeList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		foos boolean (0..*)
                	output:
                		filteredFoos boolean (0..*)
                \t
                	set filteredFoos:
                		foos\s
                			filter [ item = True ]
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param foos\s
                	* @return filteredFoos\s
                	*/
                	public List<Boolean> evaluate(List<Boolean> foos) {
                		List<Boolean> filteredFoos = doEvaluate(foos);
                \t\t
                		return filteredFoos;
                	}
                
                	protected abstract List<Boolean> doEvaluate(List<Boolean> foos);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Boolean> doEvaluate(List<Boolean> foos) {
                			if (foos == null) {
                				foos = Collections.emptyList();
                			}
                			List<Boolean> filteredFoos = new ArrayList<>();
                			return assignOutput(filteredFoos, foos);
                		}
                \t\t
                		protected List<Boolean> assignOutput(List<Boolean> filteredFoos, List<Boolean> foos) {
                			filteredFoos = MapperC.<Boolean>of(foos)
                				.filterItemNullSafe(item -> areEqual(item, MapperS.of(true), CardinalityOperator.All).get()).getMulti();
                \t\t\t
                			return filteredFoos;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(true, true, false));
        assertEquals(2, res.size());
        // upstream hasItems(true, true) — the exact-list form is strictly stronger
        // (hamcrest ignores the duplicate, admitting [true, false]; Copilot R1)
        assertEquals(List.of(true, true), res);
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListAndInputParameter}. */
    @Test
    void shouldGenerateFunctionWithFilterListAndInputParameter() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                 		test boolean (1..1)
                	output:
                		filteredFoos Foo (0..*)
                \t
                	set filteredFoos:
                		foos\s
                			filter [ item -> include = test ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "a");
        Object foo2 = foo(classes, true, "b");
        Object foo3 = foo(classes, false, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3), true);
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2)), "hasItems(foo1, foo2)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListAndCount}. */
    @Test
    void shouldGenerateFunctionWithFilterListAndCount() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		filteredFoosCount int (1..1)
                \t
                	set filteredFoosCount:
                		foos\s
                			filter fooItem [ fooItem -> include = True ]\s
                			then count
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "a");
        Object foo2 = foo(classes, true, "b");
        Object foo3 = foo(classes, false, "c");
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(foo1, foo2, foo3));
        assertEquals(2, res);
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListAndFuncCalls}. */
    @Test
    void shouldGenerateFunctionWithFilterListAndFuncCalls() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		filteredFoos Foo (0..*)
                \t
                	set filteredFoos:
                		foos\s
                			filter [ FuncFooTest( item ) ]
                			then filter [ FuncFooTest2( item ) ]
                
                func FuncFooTest:
                 	inputs:
                 		foo Foo (1..1)
                	output:
                		result boolean (0..1)
                \t
                	set result:
                		foo -> include
                
                func FuncFooTest2:
                 	inputs:
                 		foo Foo (1..1)
                	output:
                		result boolean (0..1)
                \t
                	set result:
                		foo -> include
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "a");
        Object foo2 = foo(classes, true, "b");
        Object foo3 = foo(classes, false, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2)), "hasItems(foo1, foo2)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListAndAliasParameter}. */
    @Test
    void shouldGenerateFunctionWithFilterListAndAliasParameter() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                 		test boolean (1..1)
                	output:
                		filteredFoos Foo (0..*)
                \t
                	alias testAlias:
                		test
                \t
                	set filteredFoos:
                		foos\s
                			filter [ item -> include = testAlias ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "a");
        Object foo2 = foo(classes, true, "b");
        Object foo3 = foo(classes, false, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3), true);
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2)), "hasItems(foo1, foo2)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterAndAlias}. */
    @Test
    void shouldGenerateFunctionWithFilterAndAlias() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		filteredFooAttrs string (0..*)
                \t
                	alias filteredFoosAlias:
                		foos\s
                			filter [ item -> include = True ]
                \t
                	set filteredFooAttrs:
                		filteredFoosAlias -> attr
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "a");
        Object foo2 = foo(classes, true, "b");
        Object foo3 = foo(classes, false, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of("a", "b")), "hasItems(a, b)");
    }

    /** Upstream {@code shouldGenerateFunctionWithMultipleFilterList}. */
    @Test
    void shouldGenerateFunctionWithMultipleFilterList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo2:
                	include boolean (0..1)
                	include2 boolean (0..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo2 (0..*)
                 		test boolean (0..1)
                 		test2 boolean (0..1)
                 		test3 boolean (0..1)
                	output:
                		foo Foo2 (0..1)
                \t
                	alias filteredFoos:
                		foos\s
                			filter a [ if test exists then a -> include = test else True ]
                			then filter b [ if test2 exists then b -> include2 = test2 else True ]
                			then filter c [ if test3 exists then c -> include2 = test3 else True ]
                \t
                	set foo:
                		filteredFoos only-element
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo2(classes, true, true, "a");
        Object foo2 = foo2(classes, true, false, "b");
        Object foo3 = foo2(classes, false, true, "c");
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class,
                List.of(foo1, foo2, foo3), true, true, true);
        assertEquals(foo1, res);
    }

    /** Upstream {@code shouldGenerateFunctionWithMultipleFilterList2}. */
    @Test
    void shouldGenerateFunctionWithMultipleFilterList2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo2:
                	include boolean (0..1)
                	include2 boolean (0..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo2 (0..*)
                 		test boolean (0..1)
                 		test2 boolean (0..1)
                 		test3 boolean (0..1)
                	output:
                		foo Foo2 (0..1)
                \t
                	alias filteredFoos:
                		foos\s
                			filter [ if test exists then item -> include = test else True ]
                			then filter [ if test2 exists then item -> include2 = test2 else True ]
                			then filter [ if test3 exists then item -> include2 = test3 else True ]
                \t
                	set foo:
                		filteredFoos only-element
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo2(classes, true, true, "a");
        Object foo2 = foo2(classes, true, false, "b");
        Object foo3 = foo2(classes, false, true, "c");
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class,
                List.of(foo1, foo2, foo3), true, true, true);
        assertEquals(foo1, res);
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListAliasAndOnlyElement}. */
    @Test
    void shouldGenerateFunctionWithFilterListAliasAndOnlyElement() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bar Bar (1..1)
                	output:
                		foos Foo (0..*)
                \t
                	set foos:
                		bar -> foos\s
                			extract [ if item -> include = True then Foo { include: include, attr: attr + "_bar" } else item ]
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.expression.MapperMaths;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Bar;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param bar\s
                	* @return foos\s
                	*/
                	public List<? extends Foo> evaluate(Bar bar) {
                		List<Foo.FooBuilder> foosBuilder = doEvaluate(bar);
                \t\t
                		final List<? extends Foo> foos;
                		if (foosBuilder == null) {
                			foos = null;
                		} else {
                			foos = foosBuilder.stream().map(Foo::build).collect(Collectors.toList());
                			objectValidator.validate(Foo.class, foos);
                		}
                \t\t
                		return foos;
                	}
                
                	protected abstract List<Foo.FooBuilder> doEvaluate(Bar bar);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Foo.FooBuilder> doEvaluate(Bar bar) {
                			List<Foo.FooBuilder> foos = new ArrayList<>();
                			return assignOutput(foos, bar);
                		}
                \t\t
                		protected List<Foo.FooBuilder> assignOutput(List<Foo.FooBuilder> foos, Bar bar) {
                			foos = toBuilder(MapperS.of(bar).<Foo>mapC("getFoos", _bar -> _bar.getFoos())
                				.mapItem(item -> {
                					if (areEqual(item.<Boolean>map("getInclude", foo -> foo.getInclude()), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
                						return MapperS.of(Foo.builder()
                							.setInclude(item.<Boolean>map("getInclude", foo -> foo.getInclude()).get())
                							.setAttr(MapperMaths.<String, String, String>add(item.<String>map("getAttr", foo -> foo.getAttr()), MapperS.of("_bar")).get())
                							.build());
                					}
                					return item;
                				}).getMulti());
                \t\t\t
                			return Optional.ofNullable(foos)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "foo");
        Object foo2 = foo(classes, false, "foo");
        Object barIn = bar(classes, List.of(foo1, foo2, foo2));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, barIn);
        assertEquals(3, res.size());
        Object expectedNewFoo = foo(classes, true, "foo_bar");
        assertTrue(res.containsAll(List.of(expectedNewFoo, foo2)), "hasItems(expectedNewFoo, foo2)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListAliasAndOnlyElement2}. */
    @Test
    void shouldGenerateFunctionWithFilterListAliasAndOnlyElement2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bar Bar (1..1)
                	output:
                		updatedBar Bar (1..1)
                \t
                	add updatedBar -> foos:
                		bar -> foos\s
                			extract [ if item -> include = True then Create_Foo( item -> include, Create_Attr( item -> attr, "_bar" ) ) else item ]
                
                func Create_Foo:
                	inputs:
                		include boolean (1..1)
                		attr string (1..1)
                	output:
                		foo Foo (1..1)
                \t
                	set foo -> include: include
                	set foo -> attr: attr
                
                func Create_Attr:
                	inputs:
                		s1 string (1..1)
                		s2 string (1..1)
                	output:
                		out string (1..1)
                	set out:
                		s1 + s2
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "foo");
        Object foo2 = foo(classes, false, "foo");
        Object barIn = bar(classes, List.of(foo1, foo2, foo2));
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, barIn);
        Object expectedBar = bar(classes, List.of(foo(classes, true, "foo_bar"), foo2, foo2));
        assertEquals(expectedBar, res);
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListAndOnlyElement}. */
    @Test
    void shouldGenerateFunctionWithFilterListAndOnlyElement() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		filteredFoosOnlyElement Foo (0..1)
                \t
                	set filteredFoosOnlyElement:
                		foos\s
                			filter fooItem [ fooItem -> include = True ]
                			then only-element
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "a");
        Object foo2 = foo(classes, false, "b");
        Object foo3 = foo(classes, false, "c");
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, List.of(foo1, foo2, foo3));
        assertEquals(foo1, res);
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListAndDistinct}. */
    @Test
    void shouldGenerateFunctionWithFilterListAndDistinct() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		filteredFoosDistinct Foo (0..*)
                \t
                	set filteredFoosDistinct:
                		foos\s
                			filter fooItem [ fooItem -> include = True ]
                			then distinct
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "a");
        Object foo2 = foo(classes, true, "b");
        Object foo3 = foo(classes, true, "b");
        Object foo4 = foo(classes, false, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3, foo4));
        assertEquals(2, res.size());
        assertTrue(res.contains(foo2), "hasItems(foo2)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterListAndPath} — upstream-@Disabled, carried (generation-only body; never runs). */
    @Test
    @Disabled("upstream-@Disabled: \"Add syntax support\" (the filter-on-path shape)")
    void shouldGenerateFunctionWithFilterListAndPath() {
        UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		filteredFooAttr string (0..*)
                \t
                	set filteredFooAttr:
                		foos\s
                			filter fooItem [ fooItem -> include = True ]
                				-> attr
                """);
    }

    /** Upstream {@code shouldGenerateFunctionWithNestedFilters}. */
    @Test
    void shouldGenerateFunctionWithNestedFilters() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		filteredBars Bar (0..*)
                \t
                	set filteredBars:
                		bars\s
                			filter bar [ bar -> foos\s
                				filter foo [ foo -> include = True ]\s
                					then count = 2 ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "foo");
        Object foo2 = foo(classes, false, "foo");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo2)); // count 1
        Object bar2 = bar(classes, List.of(foo1, foo1, foo2)); // count 2
        Object bar3 = bar(classes, List.of(foo1, foo1, foo1)); // count 3
        Object bar4 = bar(classes, List.of(foo2, foo1, foo1)); // count 2
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class,
                List.of(bar1, bar2, bar3, bar4));
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of(bar2, bar4)), "hasItems(bar2, bar4)");
    }

    /** Upstream {@code shouldGenerateFunctionWithExtractList}. */
    @Test
    void shouldGenerateFunctionWithExtractList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		strings string (0..*)
                \t
                	set strings:
                		foos\s
                			extract [ item -> attr ]
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param foos\s
                	* @return strings\s
                	*/
                	public List<String> evaluate(List<? extends Foo> foos) {
                		List<String> strings = doEvaluate(foos);
                \t\t
                		return strings;
                	}
                
                	protected abstract List<String> doEvaluate(List<? extends Foo> foos);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<String> doEvaluate(List<? extends Foo> foos) {
                			if (foos == null) {
                				foos = Collections.emptyList();
                			}
                			List<String> strings = new ArrayList<>();
                			return assignOutput(strings, foos);
                		}
                \t\t
                		protected List<String> assignOutput(List<String> strings, List<? extends Foo> foos) {
                			strings = MapperC.<Foo>of(foos)
                				.mapItem(item -> item.<String>map("getAttr", foo -> foo.getAttr())).getMulti();
                \t\t\t
                			return strings;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of("a", "b", "c")), "hasItems(a, b, c)");
    }

    /** Upstream {@code shouldGenerateFunctionWithExtractList2}. */
    @Test
    void shouldGenerateFunctionWithExtractList2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		strings string (0..*)
                \t
                	set strings:
                		foos\s
                			extract foo [ foo -> attr ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of("a", "b", "c")), "hasItems(a, b, c)");
    }

    /** Upstream {@code shouldGenerateFunctionWithExtractListOfListThenExtractToListOfCounts}. */
    @Test
    void shouldGenerateFunctionWithExtractListOfListThenExtractToListOfCounts() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		fooCounts int (0..*)
                \t
                	set fooCounts:
                		bars\s
                			extract bar [ bar -> foos ]
                			then extract fooListItem [ fooListItem count ]
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperListOfLists;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Bar;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param bars\s
                	* @return fooCounts\s
                	*/
                	public List<Integer> evaluate(List<? extends Bar> bars) {
                		List<Integer> fooCounts = doEvaluate(bars);
                \t\t
                		return fooCounts;
                	}
                
                	protected abstract List<Integer> doEvaluate(List<? extends Bar> bars);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Integer> doEvaluate(List<? extends Bar> bars) {
                			if (bars == null) {
                				bars = Collections.emptyList();
                			}
                			List<Integer> fooCounts = new ArrayList<>();
                			return assignOutput(fooCounts, bars);
                		}
                \t\t
                		protected List<Integer> assignOutput(List<Integer> fooCounts, List<? extends Bar> bars) {
                			final MapperListOfLists<Foo> thenArg = MapperC.<Bar>of(bars)
                				.mapItemToList(bar -> bar.<Foo>mapC("getFoos", _bar -> _bar.getFoos()));
                			fooCounts = thenArg
                				.mapListToItem(fooListItem -> MapperS.of(fooListItem.resultCount())).getMulti();
                \t\t\t
                			return fooCounts;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo1, foo2));
        Object bar3 = bar(classes, List.of(foo1));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of(3, 2, 1)), "hasItems(3, 2, 1)");
    }

    /** Upstream {@code shouldGenerateFunctionWithExtractListOfListThenExtractToListOfCounts2}. */
    @Test
    void shouldGenerateFunctionWithExtractListOfListThenExtractToListOfCounts2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		fooCounts int (0..*)
                \t
                	set fooCounts:
                		bars\s
                			extract [ item -> foos ]
                			then extract [ item count ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo1, foo2));
        Object bar3 = bar(classes, List.of(foo1));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of(3, 2, 1)), "hasItems(3, 2, 1)");
    }

    /** Upstream {@code shouldGenerateFunctionWithExtractListOfListThenFilterOnCount}. */
    @Test
    void shouldGenerateFunctionWithExtractListOfListThenFilterOnCount() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		fooCounts int (0..*)
                \t
                	set fooCounts:
                		bars\s
                			extract [ item -> foos ]
                			then filter [ item count > 1 ]
                			then extract [ item count ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo1, foo2));
        Object bar3 = bar(classes, List.of(foo1));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of(3, 2)), "hasItems(3, 2)");
    }

    /** Upstream {@code shouldGenerateFunctionWithMapListOfListThenFilterOnCount2}. */
    @Test
    void shouldGenerateFunctionWithMapListOfListThenFilterOnCount2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		fooCounts int (0..*)
                \t
                	set fooCounts:
                		bars\s
                			extract a [ a -> foos ]
                			then filter b [ b count > 1 ]
                			then extract c [ c count ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo1, foo2));
        Object bar3 = bar(classes, List.of(foo1));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of(3, 2)), "hasItems(3, 2)");
    }

    /** Upstream {@code shouldGenerateFunctionWithExtractListOfListsThenFlatten}. */
    @Test
    void shouldGenerateFunctionWithExtractListOfListsThenFlatten() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		foos Foo (0..*)
                \t
                	set foos:
                		bars\s
                			extract bar [ bar -> foos ]
                			then flatten
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperListOfLists;
                import com.rosetta.test.model.Bar;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param bars\s
                	* @return foos\s
                	*/
                	public List<? extends Foo> evaluate(List<? extends Bar> bars) {
                		List<Foo.FooBuilder> foosBuilder = doEvaluate(bars);
                \t\t
                		final List<? extends Foo> foos;
                		if (foosBuilder == null) {
                			foos = null;
                		} else {
                			foos = foosBuilder.stream().map(Foo::build).collect(Collectors.toList());
                			objectValidator.validate(Foo.class, foos);
                		}
                \t\t
                		return foos;
                	}
                
                	protected abstract List<Foo.FooBuilder> doEvaluate(List<? extends Bar> bars);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Foo.FooBuilder> doEvaluate(List<? extends Bar> bars) {
                			if (bars == null) {
                				bars = Collections.emptyList();
                			}
                			List<Foo.FooBuilder> foos = new ArrayList<>();
                			return assignOutput(foos, bars);
                		}
                \t\t
                		protected List<Foo.FooBuilder> assignOutput(List<Foo.FooBuilder> foos, List<? extends Bar> bars) {
                			final MapperListOfLists<Foo> thenArg = MapperC.<Bar>of(bars)
                				.mapItemToList(bar -> bar.<Foo>mapC("getFoos", _bar -> _bar.getFoos()));
                			foos = toBuilder(thenArg
                				.flattenList().getMulti());
                \t\t\t
                			return Optional.ofNullable(foos)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object foo4 = foo(classes, "d");
        Object foo5 = foo(classes, "e");
        Object foo6 = foo(classes, "f");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo4, foo5));
        Object bar3 = bar(classes, List.of(foo6));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(6, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2, foo3, foo4, foo5, foo6)), "hasItems(foo1..foo6)");
    }

    /** Upstream {@code shouldGenerateFunctionWithExtractListOfListsThenFlatten2}. */
    @Test
    void shouldGenerateFunctionWithExtractListOfListsThenFlatten2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		foos Foo (0..*)
                \t
                	set foos:
                		bars\s
                			extract [ item -> foos ]
                			then flatten
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object foo4 = foo(classes, "d");
        Object foo5 = foo(classes, "e");
        Object foo6 = foo(classes, "f");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo4, foo5));
        Object bar3 = bar(classes, List.of(foo6));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(6, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2, foo3, foo4, foo5, foo6)), "hasItems(foo1..foo6)");
    }

    /** Upstream {@code shouldGenerateFunctionWithExtractListOfListsThenFlatten3}. */
    @Test
    void shouldGenerateFunctionWithExtractListOfListsThenFlatten3() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		attrs string (0..*)
                \t
                	set attrs:
                		bars\s
                			extract [ item -> foos ]
                			then flatten
                			then extract [ item -> attr ]
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperListOfLists;
                import com.rosetta.test.model.Bar;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param bars\s
                	* @return attrs\s
                	*/
                	public List<String> evaluate(List<? extends Bar> bars) {
                		List<String> attrs = doEvaluate(bars);
                \t\t
                		return attrs;
                	}
                
                	protected abstract List<String> doEvaluate(List<? extends Bar> bars);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<String> doEvaluate(List<? extends Bar> bars) {
                			if (bars == null) {
                				bars = Collections.emptyList();
                			}
                			List<String> attrs = new ArrayList<>();
                			return assignOutput(attrs, bars);
                		}
                \t\t
                		protected List<String> assignOutput(List<String> attrs, List<? extends Bar> bars) {
                			final MapperListOfLists<Foo> thenArg0 = MapperC.<Bar>of(bars)
                				.mapItemToList(item -> item.<Foo>mapC("getFoos", bar -> bar.getFoos()));
                			final MapperC<Foo> thenArg1 = thenArg0
                				.flattenList();
                			attrs = thenArg1
                				.mapItem(item -> item.<String>map("getAttr", foo -> foo.getAttr())).getMulti();
                \t\t\t
                			return attrs;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object foo4 = foo(classes, "d");
        Object foo5 = foo(classes, "e");
        Object foo6 = foo(classes, "f");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo4, foo5));
        Object bar3 = bar(classes, List.of(foo6));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(6, res.size());
        assertTrue(res.containsAll(List.of("a", "b", "c", "d", "e", "f")), "hasItems(a..f)");
    }

    /** Upstream {@code shouldGenerateFunctionWithExtractListCount}. */
    @Test
    void shouldGenerateFunctionWithExtractListCount() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		fooCounts int (0..*)
                \t
                	set fooCounts:
                		bars\s
                			extract [ item -> foos count ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo1, foo2));
        Object bar3 = bar(classes, List.of(foo1));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of(3, 2, 1)), "hasItems(3, 2, 1)");
    }

    /** Upstream {@code shouldGenerateFunctionWithMapListCount2}. */
    @Test
    void shouldGenerateFunctionWithMapListCount2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		fooCounts int (0..*)
                \t
                	set fooCounts:
                		bars\s
                			extract bar [ bar -> foos count ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo1, foo2));
        Object bar3 = bar(classes, List.of(foo1));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of(3, 2, 1)), "hasItems(3, 2, 1)");
    }

    /** Upstream {@code shouldGenerateFunctionWithNestedExtracts}. */
    @Test
    void shouldGenerateFunctionWithNestedExtracts() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		updatedBars Bar (0..*)
                \t
                	set updatedBars:
                		bars\s
                			extract bar [ bar -> foos\s
                				extract foo [ NewFoo( foo -> attr + "_bar" ) ]
                			]
                			then extract updatedFoos [ NewBar( updatedFoos ) ]
                
                func NewBar:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		bar Bar (1..1)
                \t
                	set bar -> foos:
                		foos
                
                func NewFoo:
                 	inputs:
                 		attr string (1..1)
                	output:
                		foo Foo (0..1)
                \t
                	set foo -> attr:
                		attr
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.MapperMaths;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperListOfLists;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Bar;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected NewBar newBar;
                	@Inject protected NewFoo newFoo;
                
                	/**
                	* @param bars\s
                	* @return updatedBars\s
                	*/
                	public List<? extends Bar> evaluate(List<? extends Bar> bars) {
                		List<Bar.BarBuilder> updatedBarsBuilder = doEvaluate(bars);
                \t\t
                		final List<? extends Bar> updatedBars;
                		if (updatedBarsBuilder == null) {
                			updatedBars = null;
                		} else {
                			updatedBars = updatedBarsBuilder.stream().map(Bar::build).collect(Collectors.toList());
                			objectValidator.validate(Bar.class, updatedBars);
                		}
                \t\t
                		return updatedBars;
                	}
                
                	protected abstract List<Bar.BarBuilder> doEvaluate(List<? extends Bar> bars);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Bar.BarBuilder> doEvaluate(List<? extends Bar> bars) {
                			if (bars == null) {
                				bars = Collections.emptyList();
                			}
                			List<Bar.BarBuilder> updatedBars = new ArrayList<>();
                			return assignOutput(updatedBars, bars);
                		}
                \t\t
                		protected List<Bar.BarBuilder> assignOutput(List<Bar.BarBuilder> updatedBars, List<? extends Bar> bars) {
                			final MapperListOfLists<Foo> thenArg = MapperC.<Bar>of(bars)
                				.mapItemToList(bar -> bar.<Foo>mapC("getFoos", _bar -> _bar.getFoos())
                					.mapItem(foo -> MapperS.of(newFoo.evaluate(MapperMaths.<String, String, String>add(foo.<String>map("getAttr", _foo -> _foo.getAttr()), MapperS.of("_bar")).get()))));
                			updatedBars = toBuilder(thenArg
                				.mapListToItem(updatedFoos -> MapperS.of(newBar.evaluate(updatedFoos.getMulti()))).getMulti());
                \t\t\t
                			return Optional.ofNullable(updatedBars)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo1, foo2));
        Object bar3 = bar(classes, List.of(foo1));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(3, res.size());
        Object expectedFoo1 = foo(classes, "a_bar");
        Object expectedFoo2 = foo(classes, "b_bar");
        Object expectedFoo3 = foo(classes, "c_bar");
        Object expectedBar1 = bar(classes, List.of(expectedFoo1, expectedFoo2, expectedFoo3));
        Object expectedBar2 = bar(classes, List.of(expectedFoo1, expectedFoo2));
        Object expectedBar3 = bar(classes, List.of(expectedFoo1));
        assertTrue(res.containsAll(List.of(expectedBar1, expectedBar2, expectedBar3)),
                "hasItems(expectedBar1, expectedBar2, expectedBar3)");
    }

    /** Upstream {@code shouldGenerateFunctionWithNestedMaps2}. */
    @Test
    void shouldGenerateFunctionWithNestedMaps2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		updatedBars Bar (0..*)
                \t
                	set updatedBars:
                		bars\s
                			extract bar [\s
                				NewBar( bar -> foos\s
                					extract foo [ NewFoo( foo -> attr + "_bar" ) ] )
                			]
                
                func NewBar:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		bar Bar (1..1)
                \t
                	set bar -> foos:
                		foos
                
                func NewFoo:
                 	inputs:
                 		attr string (1..1)
                	output:
                		foo Foo (0..1)
                \t
                	set foo -> attr:
                		attr
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.MapperMaths;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Bar;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected NewBar newBar;
                	@Inject protected NewFoo newFoo;
                
                	/**
                	* @param bars\s
                	* @return updatedBars\s
                	*/
                	public List<? extends Bar> evaluate(List<? extends Bar> bars) {
                		List<Bar.BarBuilder> updatedBarsBuilder = doEvaluate(bars);
                \t\t
                		final List<? extends Bar> updatedBars;
                		if (updatedBarsBuilder == null) {
                			updatedBars = null;
                		} else {
                			updatedBars = updatedBarsBuilder.stream().map(Bar::build).collect(Collectors.toList());
                			objectValidator.validate(Bar.class, updatedBars);
                		}
                \t\t
                		return updatedBars;
                	}
                
                	protected abstract List<Bar.BarBuilder> doEvaluate(List<? extends Bar> bars);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Bar.BarBuilder> doEvaluate(List<? extends Bar> bars) {
                			if (bars == null) {
                				bars = Collections.emptyList();
                			}
                			List<Bar.BarBuilder> updatedBars = new ArrayList<>();
                			return assignOutput(updatedBars, bars);
                		}
                \t\t
                		protected List<Bar.BarBuilder> assignOutput(List<Bar.BarBuilder> updatedBars, List<? extends Bar> bars) {
                			updatedBars = toBuilder(MapperC.<Bar>of(bars)
                				.mapItem(bar -> MapperS.of(newBar.evaluate(bar.<Foo>mapC("getFoos", _bar -> _bar.getFoos())
                					.mapItem(foo -> MapperS.of(newFoo.evaluate(MapperMaths.<String, String, String>add(foo.<String>map("getAttr", _foo -> _foo.getAttr()), MapperS.of("_bar")).get()))).getMulti()))).getMulti());
                \t\t\t
                			return Optional.ofNullable(updatedBars)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object bar1 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar2 = bar(classes, List.of(foo1, foo2));
        Object bar3 = bar(classes, List.of(foo1));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3));
        assertEquals(3, res.size());
        Object expectedFoo1 = foo(classes, "a_bar");
        Object expectedFoo2 = foo(classes, "b_bar");
        Object expectedFoo3 = foo(classes, "c_bar");
        Object expectedBar1 = bar(classes, List.of(expectedFoo1, expectedFoo2, expectedFoo3));
        Object expectedBar2 = bar(classes, List.of(expectedFoo1, expectedFoo2));
        Object expectedBar3 = bar(classes, List.of(expectedFoo1));
        assertTrue(res.containsAll(List.of(expectedBar1, expectedBar2, expectedBar3)),
                "hasItems(expectedBar1, expectedBar2, expectedBar3)");
    }

    /** Upstream {@code shouldGenerateFunctionWithExtractListModifyItemFunc}. */
    @Test
    void shouldGenerateFunctionWithExtractListModifyItemFunc() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		updatedFoos Foo (0..*)
                \t
                	set updatedFoos:
                		foos\s
                			extract [ NewFoo( item -> attr + "_1" ) ]
                
                func NewFoo:
                 	inputs:
                 		attr string (1..1)
                	output:
                		foo Foo (0..1)
                \t
                	set foo -> attr:
                		attr
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(3, res.size());
        Object expectedFoo1 = foo(classes, "a_1");
        Object expectedFoo2 = foo(classes, "b_1");
        Object expectedFoo3 = foo(classes, "c_1");
        assertTrue(res.containsAll(List.of(expectedFoo1, expectedFoo2, expectedFoo3)), "hasItems(a_1, b_1, c_1)");
    }

    /** Upstream {@code shouldGenerateFunctionWithFilterThenExtract}. */
    @Test
    void shouldGenerateFunctionWithFilterThenExtract() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	include boolean (1..1)
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		newFoos string (0..*)
                \t
                	set newFoos:
                		foos\s
                			filter [ item -> include = True ]
                			then extract [ item -> attr ]
                
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param foos\s
                	* @return newFoos\s
                	*/
                	public List<String> evaluate(List<? extends Foo> foos) {
                		List<String> newFoos = doEvaluate(foos);
                \t\t
                		return newFoos;
                	}
                
                	protected abstract List<String> doEvaluate(List<? extends Foo> foos);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<String> doEvaluate(List<? extends Foo> foos) {
                			if (foos == null) {
                				foos = Collections.emptyList();
                			}
                			List<String> newFoos = new ArrayList<>();
                			return assignOutput(newFoos, foos);
                		}
                \t\t
                		protected List<String> assignOutput(List<String> newFoos, List<? extends Foo> foos) {
                			final MapperC<Foo> thenArg = MapperC.<Foo>of(foos)
                				.filterItemNullSafe(item -> areEqual(item.<Boolean>map("getInclude", foo -> foo.getInclude()), MapperS.of(true), CardinalityOperator.All).get());
                			newFoos = thenArg
                				.mapItem(item -> item.<String>map("getAttr", foo -> foo.getAttr())).getMulti();
                \t\t\t
                			return newFoos;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, true, "a");
        Object foo2 = foo(classes, true, "b");
        Object foo3 = foo(classes, false, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3));
        assertEquals(2, res.size());
        assertTrue(res.containsAll(List.of("a", "b")), "hasItems(a, b)");
    }

    /** Upstream {@code shouldGenerateFunctionWithSameNamespace}. */
    @Test
    void shouldGenerateFunctionWithSameNamespace() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace ns1
                
                type Bar:
                	barAttr string (1..1)
                
                type Foo:
                	fooAttr string (1..1)
                
                func GetFoo:
                	inputs:
                		barAttr string (1..1)
                	output:
                		foo Foo (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		strings string (0..*)
                \t
                	set strings:
                		bars\s
                			extract [ GetFoo( item -> barAttr ) ]
                			then extract [ item -> fooAttr ]
                """);
        assertEquals("""
                package ns1.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import javax.inject.Inject;
                import ns1.Bar;
                import ns1.Foo;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected GetFoo getFoo;
                
                	/**
                	* @param bars\s
                	* @return strings\s
                	*/
                	public List<String> evaluate(List<? extends Bar> bars) {
                		List<String> strings = doEvaluate(bars);
                \t\t
                		return strings;
                	}
                
                	protected abstract List<String> doEvaluate(List<? extends Bar> bars);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<String> doEvaluate(List<? extends Bar> bars) {
                			if (bars == null) {
                				bars = Collections.emptyList();
                			}
                			List<String> strings = new ArrayList<>();
                			return assignOutput(strings, bars);
                		}
                \t\t
                		protected List<String> assignOutput(List<String> strings, List<? extends Bar> bars) {
                			final MapperC<Foo> thenArg = MapperC.<Bar>of(bars)
                				.mapItem(item -> MapperS.of(getFoo.evaluate(item.<String>map("getBarAttr", bar -> bar.getBarAttr()).get())));
                			strings = thenArg
                				.mapItem(item -> item.<String>map("getFooAttr", foo -> foo.getFooAttr())).getMulti();
                \t\t\t
                			return strings;
                		}
                	}
                }
                """, code.get("ns1.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFunctionWithDifferentNamespace}. */
    @Test
    void shouldGenerateFunctionWithDifferentNamespace() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace ns1
                
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                """, """
                namespace ns2
                
                import ns1.*
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		strings string (0..*)
                \t
                	set strings:
                		bars\s
                			extract [ item -> foos ]
                			then flatten
                			then extract [ item -> attr ]
                """);
        assertEquals("""
                package ns2.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperListOfLists;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import ns1.Bar;
                import ns1.Foo;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param bars\s
                	* @return strings\s
                	*/
                	public List<String> evaluate(List<? extends Bar> bars) {
                		List<String> strings = doEvaluate(bars);
                \t\t
                		return strings;
                	}
                
                	protected abstract List<String> doEvaluate(List<? extends Bar> bars);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<String> doEvaluate(List<? extends Bar> bars) {
                			if (bars == null) {
                				bars = Collections.emptyList();
                			}
                			List<String> strings = new ArrayList<>();
                			return assignOutput(strings, bars);
                		}
                \t\t
                		protected List<String> assignOutput(List<String> strings, List<? extends Bar> bars) {
                			final MapperListOfLists<Foo> thenArg0 = MapperC.<Bar>of(bars)
                				.mapItemToList(item -> item.<Foo>mapC("getFoos", bar -> bar.getFoos()));
                			final MapperC<Foo> thenArg1 = thenArg0
                				.flattenList();
                			strings = thenArg1
                				.mapItem(item -> item.<String>map("getAttr", foo -> foo.getAttr())).getMulti();
                \t\t\t
                			return strings;
                		}
                	}
                }
                """, code.get("ns2.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFunctionWithDifferentNamespace2}. */
    @Test
    void shouldGenerateFunctionWithDifferentNamespace2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace ns1
                
                type Bar:
                	barAttr string (1..1)
                
                type Foo:
                	fooAttr string (1..1)
                
                func GetFoo:
                	inputs:
                		barAttr string (1..1)
                	output:
                		foo Foo (1..1)
                """, """
                namespace ns2
                
                import ns1.*
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		strings string (0..*)
                \t
                	set strings:
                		bars\s
                			extract [ GetFoo( item -> barAttr ) ]
                			then extract [ item -> fooAttr ]
                """);
        assertEquals("""
                package ns2.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import javax.inject.Inject;
                import ns1.Bar;
                import ns1.Foo;
                import ns1.functions.GetFoo;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected GetFoo getFoo;
                
                	/**
                	* @param bars\s
                	* @return strings\s
                	*/
                	public List<String> evaluate(List<? extends Bar> bars) {
                		List<String> strings = doEvaluate(bars);
                \t\t
                		return strings;
                	}
                
                	protected abstract List<String> doEvaluate(List<? extends Bar> bars);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<String> doEvaluate(List<? extends Bar> bars) {
                			if (bars == null) {
                				bars = Collections.emptyList();
                			}
                			List<String> strings = new ArrayList<>();
                			return assignOutput(strings, bars);
                		}
                \t\t
                		protected List<String> assignOutput(List<String> strings, List<? extends Bar> bars) {
                			final MapperC<Foo> thenArg = MapperC.<Bar>of(bars)
                				.mapItem(item -> MapperS.of(getFoo.evaluate(item.<String>map("getBarAttr", bar -> bar.getBarAttr()).get())));
                			strings = thenArg
                				.mapItem(item -> item.<String>map("getFooAttr", foo -> foo.getFooAttr())).getMulti();
                \t\t\t
                			return strings;
                		}
                	}
                }
                """, code.get("ns2.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFunctionWithDifferentNamespace3}. */
    @Test
    void shouldGenerateFunctionWithDifferentNamespace3() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace ns1
                
                type Bar:
                	barAttr string (1..1)
                
                type Foo:
                	fooAttr string (1..1)
                
                type Baz:
                	fooAttr string (1..1)
                
                func GetFoo:
                	inputs:
                		baz Baz (1..1)
                	output:
                		foo Foo (1..1)
                
                func GetBaz:
                	inputs:
                		attr string (1..1)
                	output:
                		baz Baz (1..1)
                """, """
                namespace ns2
                
                import ns1.*
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		strings string (0..*)
                \t
                	set strings:
                		bars\s
                			extract [ GetFoo( GetBaz( item -> barAttr ) ) ]
                			then extract [ item -> fooAttr ]
                """);
        assertEquals("""
                package ns2.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import javax.inject.Inject;
                import ns1.Bar;
                import ns1.Foo;
                import ns1.functions.GetBaz;
                import ns1.functions.GetFoo;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected GetBaz getBaz;
                	@Inject protected GetFoo getFoo;
                
                	/**
                	* @param bars\s
                	* @return strings\s
                	*/
                	public List<String> evaluate(List<? extends Bar> bars) {
                		List<String> strings = doEvaluate(bars);
                \t\t
                		return strings;
                	}
                
                	protected abstract List<String> doEvaluate(List<? extends Bar> bars);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<String> doEvaluate(List<? extends Bar> bars) {
                			if (bars == null) {
                				bars = Collections.emptyList();
                			}
                			List<String> strings = new ArrayList<>();
                			return assignOutput(strings, bars);
                		}
                \t\t
                		protected List<String> assignOutput(List<String> strings, List<? extends Bar> bars) {
                			final MapperC<Foo> thenArg = MapperC.<Bar>of(bars)
                				.mapItem(item -> MapperS.of(getFoo.evaluate(getBaz.evaluate(item.<String>map("getBarAttr", bar -> bar.getBarAttr()).get()))));
                			strings = thenArg
                				.mapItem(item -> item.<String>map("getFooAttr", foo -> foo.getFooAttr())).getMulti();
                \t\t\t
                			return strings;
                		}
                	}
                }
                """, code.get("ns2.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateListWithinIf}. */
    @Test
    void shouldGenerateListWithinIf() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                 		test string (1..1)
                	output:
                		strings string (0..*)
                \t
                	set strings:
                		if test = "a"
                		then foos extract [ item -> attr + "_a" ]
                		else if test = "b"
                		then foos extract [ item -> attr + "_b" ]
                		else if test = "c"
                		then foos extract [ item -> attr + "_c" ]
                		// default else
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.expression.MapperMaths;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param foos\s
                	* @param test\s
                	* @return strings\s
                	*/
                	public List<String> evaluate(List<? extends Foo> foos, String test) {
                		List<String> strings = doEvaluate(foos, test);
                \t\t
                		return strings;
                	}
                
                	protected abstract List<String> doEvaluate(List<? extends Foo> foos, String test);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<String> doEvaluate(List<? extends Foo> foos, String test) {
                			if (foos == null) {
                				foos = Collections.emptyList();
                			}
                			List<String> strings = new ArrayList<>();
                			return assignOutput(strings, foos, test);
                		}
                \t\t
                		protected List<String> assignOutput(List<String> strings, List<? extends Foo> foos, String test) {
                			if (areEqual(MapperS.of(test), MapperS.of("a"), CardinalityOperator.All).getOrDefault(false)) {
                				strings = MapperC.<Foo>of(foos)
                					.mapItem(item -> MapperMaths.<String, String, String>add(item.<String>map("getAttr", foo -> foo.getAttr()), MapperS.of("_a"))).getMulti();
                			} else if (areEqual(MapperS.of(test), MapperS.of("b"), CardinalityOperator.All).getOrDefault(false)) {
                				strings = MapperC.<Foo>of(foos)
                					.mapItem(item -> MapperMaths.<String, String, String>add(item.<String>map("getAttr", foo -> foo.getAttr()), MapperS.of("_b"))).getMulti();
                			} else if (areEqual(MapperS.of(test), MapperS.of("c"), CardinalityOperator.All).getOrDefault(false)) {
                				strings = MapperC.<Foo>of(foos)
                					.mapItem(item -> MapperMaths.<String, String, String>add(item.<String>map("getAttr", foo -> foo.getAttr()), MapperS.of("_c"))).getMulti();
                			} else {
                				strings = Collections.<String>emptyList();
                			}
                \t\t\t
                			return strings;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2, foo3), "b");
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of("a_b", "b_b", "c_b")), "hasItems(a_b, b_b, c_b)");
    }

    /** Upstream {@code shouldGenerateListJoin}. */
    @Test
    void shouldGenerateListJoin() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		stringList string (0..*)
                	output:
                		concatenatedString string (1..1)
                \t
                	set concatenatedString:
                		stringList
                			join
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        String res = UpstreamPortHarness.invokeFunc(func, String.class, List.of("a", "b", "c", "d", "e"));
        assertEquals("abcde", res);
    }

    /** Upstream {@code shouldGenerateListJoinWithDelimiter}. */
    @Test
    void shouldGenerateListJoinWithDelimiter() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		stringList string (0..*)
                	output:
                		concatenatedString string (1..1)
                \t
                	set concatenatedString:
                		stringList
                			join "_"
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        String res = UpstreamPortHarness.invokeFunc(func, String.class, List.of("a", "b", "c", "d", "e"));
        assertEquals("a_b_c_d_e", res);
    }

    /** Upstream {@code shouldGenerateListReduceString}. */
    @Test
    void shouldGenerateListReduceString() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		stringList string (0..*)
                	output:
                		concatenatedString string (1..1)
                \t
                	set concatenatedString:
                		stringList
                			reduce a, b [ a + b ]
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.MapperMaths;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import java.util.Collections;
                import java.util.List;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param stringList\s
                	* @return concatenatedString\s
                	*/
                	public String evaluate(List<String> stringList) {
                		String concatenatedString = doEvaluate(stringList);
                \t\t
                		return concatenatedString;
                	}
                
                	protected abstract String doEvaluate(List<String> stringList);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected String doEvaluate(List<String> stringList) {
                			if (stringList == null) {
                				stringList = Collections.emptyList();
                			}
                			String concatenatedString = null;
                			return assignOutput(concatenatedString, stringList);
                		}
                \t\t
                		protected String assignOutput(String concatenatedString, List<String> stringList) {
                			concatenatedString = MapperC.<String>of(stringList)
                				.<String>reduce((a, b) -> MapperMaths.<String, String, String>add(a, b)).get();
                \t\t\t
                			return concatenatedString;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        String res = UpstreamPortHarness.invokeFunc(func, String.class, List.of("a", "b", "c", "d", "e"));
        assertEquals("abcde", res);
    }

    /** Upstream {@code shouldGenerateListSumInt}. */
    @Test
    void shouldGenerateListSumInt() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		intList int (0..*)
                	output:
                		total int (1..1)
                \t
                	set total:
                		intList
                			sum
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        List<?> ints = List.of(1, 3, 5, 7, 11);
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, ints);
        assertEquals(27, res);
    }

    /** Upstream {@code shouldGenerateListSumBigDecimal}. */
    @Test
    void shouldGenerateListSumBigDecimal() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		numberList number (0..*)
                	output:
                		total number (1..1)
                \t
                	set total:
                		numberList
                			sum
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        List<?> nums = List.of(BigDecimal.valueOf(1.1), BigDecimal.valueOf(3.1), BigDecimal.valueOf(5.1), BigDecimal.valueOf(7.1), BigDecimal.valueOf(11.1));
        BigDecimal res = UpstreamPortHarness.invokeFunc(func, BigDecimal.class, nums);
        assertEquals(BigDecimal.valueOf(27.5), res);
    }

    /** Upstream {@code shouldGenerateListReduceSum}. */
    @Test
    void shouldGenerateListReduceSum() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		intList int (0..*)
                	output:
                		total int (1..1)
                \t
                	set total:
                		intList
                			reduce a, b [ a + b ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(1, 3, 5, 7, 11));
        assertEquals(27, res);
    }

    /** Upstream {@code shouldGenerateListFirstInt}. */
    @Test
    void shouldGenerateListFirstInt() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		intList int (0..*)
                	output:
                		firstInt int (1..1)
                \t
                	set firstInt:
                		intList
                			first
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(1, 2, 3, 4, 5));
        assertEquals(1, res);
    }

    /** Upstream {@code shouldGenerateListFirstComplexType}. */
    @Test
    void shouldGenerateListFirstComplexType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		fooList Foo (0..*)
                	output:
                		firstFoo Foo (1..1)
                \t
                	set firstFoo:
                		fooList
                			first
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        List<?> foos = List.of(foo1, foo(classes, "b"), foo(classes, "c"), foo(classes, "d"), foo(classes, "e"));
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, foos);
        assertEquals(foo1, res);
    }

    /** Upstream {@code shouldGenerateListFirstComplexTypeEmptyList}. */
    @Test
    void shouldGenerateListFirstComplexTypeEmptyList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		fooList Foo (0..*)
                	output:
                		firstFoo Foo (1..1)
                \t
                	set firstFoo:
                		fooList
                			first
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, new ArrayList<>());
        assertNull(res);
    }

    /** Upstream {@code shouldGenerateListLastInt}. */
    @Test
    void shouldGenerateListLastInt() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		intList int (0..*)
                	output:
                		lastInt int (1..1)
                \t
                	set lastInt:
                		intList
                			last
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(1, 2, 3, 4, 5));
        assertEquals(5, res);
    }

    /** Upstream {@code shouldGenerateListLastComplexType}. */
    @Test
    void shouldGenerateListLastComplexType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		fooList Foo (0..*)
                	output:
                		lastFoo Foo (1..1)
                \t
                	set lastFoo:
                		fooList
                			last
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo5 = foo(classes, "e");
        List<?> foos = List.of(foo(classes, "a"), foo(classes, "b"), foo(classes, "c"), foo(classes, "d"), foo5);
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, foos);
        assertEquals(foo5, res);
    }

    /** Upstream {@code shouldGenerateListReduceSubtract}. */
    @Test
    void shouldGenerateListReduceSubtract() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		intList int (0..*)
                	output:
                		total int (1..1)
                \t
                	set total:
                		intList
                			reduce a, b [ a - b ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(10, 7, 1));
        assertEquals(2, res);
        Integer res2 = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(1, 7, 10));
        assertEquals(-16, res2);
    }

    /** Upstream {@code shouldGenerateEmptyListReduceSum}. */
    @Test
    void shouldGenerateEmptyListReduceSum() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		numberList int (0..*)
                	output:
                		total int (1..1)
                \t
                	set total:
                		numberList
                			reduce a, b [ a + b ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, new ArrayList<>());
        assertNull(res);
    }

    /** Upstream {@code shouldGenerateListReduceProduct}. */
    @Test
    void shouldGenerateListReduceProduct() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		numberList int (0..*)
                	output:
                		total int (1..1)
                \t
                	set total:
                		numberList
                			reduce a, b [ a * b ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(1, 3, 5, 7, 11));
        assertEquals(1155, res);
    }

    /** Upstream {@code shouldGenerateListReduceMaxNumber}. */
    @Test
    void shouldGenerateListReduceMaxNumber() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		numberList int (0..*)
                	output:
                		total int (1..1)
                \t
                	set total:
                		numberList
                			reduce a, b [ if a > b then a else b ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(1, 3, 5, 7, 11));
        assertEquals(11, res);
    }

    /** Upstream {@code shouldGenerateListReduceMinNumber}. */
    @Test
    void shouldGenerateListReduceMinNumber() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		numberList int (0..*)
                	output:
                		total int (1..1)
                \t
                	set total:
                		numberList
                			reduce a, b [ Min( a, b ) ]
                
                func Min:
                	inputs:
                		a int (1..1)
                		b int (1..1)
                	output:
                		result int (1..1)
                	set result:
                		if a > b then b else a
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(1, 3, 5, 7, 11));
        assertEquals(1, res);
    }

    /** Upstream {@code shouldGenerateListReduceComplexType}. */
    @Test
    void shouldGenerateListReduceComplexType() throws Exception {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		foo Foo (1..1)
                \t
                	set foo:
                		foos
                			reduce foo1, foo2 [ Create_Foo( foo1 -> attr + foo2 -> attr ) ]
                
                func Create_Foo:
                 	inputs:
                 		attr string (1..1)
                	output:
                		foo Foo (1..1)
                \t
                	set foo -> attr: attr
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.MapperMaths;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Foo;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import javax.inject.Inject;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected Create_Foo create_Foo;
                
                	/**
                	* @param foos\s
                	* @return foo\s
                	*/
                	public Foo evaluate(List<? extends Foo> foos) {
                		Foo.FooBuilder fooBuilder = doEvaluate(foos);
                \t\t
                		final Foo foo;
                		if (fooBuilder == null) {
                			foo = null;
                		} else {
                			foo = fooBuilder.build();
                			objectValidator.validate(Foo.class, foo);
                		}
                \t\t
                		return foo;
                	}
                
                	protected abstract Foo.FooBuilder doEvaluate(List<? extends Foo> foos);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected Foo.FooBuilder doEvaluate(List<? extends Foo> foos) {
                			if (foos == null) {
                				foos = Collections.emptyList();
                			}
                			Foo.FooBuilder foo = Foo.builder();
                			return assignOutput(foo, foos);
                		}
                \t\t
                		protected Foo.FooBuilder assignOutput(Foo.FooBuilder foo, List<? extends Foo> foos) {
                			foo = toBuilder(MapperC.<Foo>of(foos)
                				.<Foo>reduce((foo1, foo2) -> MapperS.of(create_Foo.evaluate(MapperMaths.<String, String, String>add(foo1.<String>map("getAttr", _foo -> _foo.getAttr()), foo2.<String>map("getAttr", _foo -> _foo.getAttr())).get()))).get());
                \t\t\t
                			return Optional.ofNullable(foo)
                				.map(o -> o.prune())
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, List.of(
                foo(classes, "a"), foo(classes, "b"), foo(classes, "c"), foo(classes, "d"), foo(classes, "e")));
        String attr = (String) res.getClass().getMethod("getAttr").invoke(res);
        assertEquals("abcde", attr);
    }

    /** Upstream {@code shouldGenerateListReduceThenMapSingle}. */
    @Test
    void shouldGenerateListReduceThenMapSingle() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		fooCount int (1..1)
                \t
                	set fooCount:
                		bars
                			reduce bar1, bar2 [ if bar1 -> foos count > bar2 -> foos count then bar1 else bar2 ]
                			then extract [ item -> foos count ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object foo4 = foo(classes, "d");
        Object bar1 = bar(classes, List.of(foo1));
        Object bar2 = bar(classes, List.of(foo1, foo2));
        Object bar3 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar4 = bar(classes, List.of(foo2, foo2, foo3, foo4));
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(bar1, bar2, bar3, bar4));
        assertEquals(4, res);
    }

    /** Upstream {@code shouldGenerateListReduceThenExtractList}. */
    @Test
    void shouldGenerateListReduceThenExtractList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bars Bar (0..*)
                	output:
                		attrs string (0..*)
                \t
                	set attrs:
                		bars
                			reduce bar1, bar2 [ if bar1 -> foos count > bar2 -> foos count then bar1 else bar2 ] // max by foo count
                			then extract [ item -> foos ]
                			then extract [ item -> attr ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object foo4 = foo(classes, "d");
        Object bar1 = bar(classes, List.of(foo1));
        Object bar2 = bar(classes, List.of(foo1, foo2));
        Object bar3 = bar(classes, List.of(foo1, foo2, foo3));
        Object bar4 = bar(classes, List.of(foo1, foo2, foo3, foo4));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(bar1, bar2, bar3, bar4));
        assertEquals(4, res.size());
        assertTrue(res.containsAll(List.of("a", "b", "c", "d")), "hasItems('a', 'b', 'c', 'd')");
    }

    /** Upstream {@code shouldGenerateListMaxInt}. */
    @Test
    void shouldGenerateListMaxInt() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		intList int (0..*)
                	output:
                		result int (0..1)
                \t
                	set result:
                		intList
                			max
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Integer res = UpstreamPortHarness.invokeFunc(func, Integer.class, List.of(1, 2, 3, 4, 5));
        assertEquals(5, res);
    }

    /** Upstream {@code shouldGenerateListMaxComplexType}. */
    @Test
    void shouldGenerateListMaxComplexType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		foo Foo (0..1)
                \t
                	set foo:
                		foos
                			max [ item -> attr ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo5 = foo(classes, "e");
        List<?> foos = List.of(foo(classes, "a"), foo(classes, "b"), foo(classes, "c"), foo(classes, "d"), foo5);
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, foos);
        assertEquals(foo5, res);
    }

    /** Upstream {@code shouldGenerateListMinBigDecimal}. */
    @Test
    void shouldGenerateListMinBigDecimal() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		numberList number (0..*)
                	output:
                		result number (0..1)
                \t
                	set result:
                		numberList
                			min
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        List<?> nums = List.of(BigDecimal.valueOf(1.1), BigDecimal.valueOf(1.2), BigDecimal.valueOf(1.3), BigDecimal.valueOf(1.4), BigDecimal.valueOf(1.5));
        BigDecimal res = UpstreamPortHarness.invokeFunc(func, BigDecimal.class, nums);
        assertEquals(BigDecimal.valueOf(1.1), res);
    }

    /** Upstream {@code shouldGenerateListMinComplexType}. */
    @Test
    void shouldGenerateListMinComplexType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                	output:
                		foo Foo (0..1)
                \t
                	set foo:
                		foos
                			min [ item -> attr ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        List<?> foos = List.of(foo1, foo(classes, "b"), foo(classes, "c"), foo(classes, "d"), foo(classes, "e"));
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, foos);
        assertEquals(foo1, res);
    }

    /** Upstream {@code shouldGenerateIntListSort}. */
    @Test
    void shouldGenerateIntListSort() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:\s
                	inputs:
                		numbers int (0..*)
                	output:
                		sortedNumbers int (0..*)
                
                	set sortedNumbers:
                		numbers sort // sort items
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param numbers\s
                	* @return sortedNumbers\s
                	*/
                	public List<Integer> evaluate(List<Integer> numbers) {
                		List<Integer> sortedNumbers = doEvaluate(numbers);
                \t\t
                		return sortedNumbers;
                	}
                
                	protected abstract List<Integer> doEvaluate(List<Integer> numbers);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Integer> doEvaluate(List<Integer> numbers) {
                			if (numbers == null) {
                				numbers = Collections.emptyList();
                			}
                			List<Integer> sortedNumbers = new ArrayList<>();
                			return assignOutput(sortedNumbers, numbers);
                		}
                \t\t
                		protected List<Integer> assignOutput(List<Integer> sortedNumbers, List<Integer> numbers) {
                			sortedNumbers = MapperC.<Integer>of(numbers)
                				.sort().getMulti();
                \t\t\t
                			return sortedNumbers;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(4, 2, 3, 1));
        assertEquals(4, res.size());
        assertEquals(List.of(1, 2, 3, 4), res);
    }

    /** Upstream {@code shouldGenerateDistinctIntListSort}. */
    @Test
    void shouldGenerateDistinctIntListSort() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:\s
                	inputs:
                		numbers int (0..*)
                	output:
                		sortedNumbers int (0..*)
                
                	set sortedNumbers:
                		numbers\s
                			distinct
                			sort
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(4, 2, 2, 4, 3, 1, 1, 3));
        assertEquals(4, res.size());
        assertEquals(List.of(1, 2, 3, 4), res);
    }

    /** Upstream {@code shouldGenerateDateListSort}. */
    @Test
    void shouldGenerateDateListSort() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:\s
                	inputs:
                		dates date (0..*)
                	output:
                		sortedDates date (0..*)
                
                	set sortedDates:
                		dates sort // sort items
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object date1 = date(classes, 2000, 1, 1);
        Object date2 = date(classes, 2000, 1, 2);
        Object date3 = date(classes, 2000, 2, 1);
        Object date4 = date(classes, 2001, 1, 1);
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(date4, date1, date2, date3));
        assertEquals(4, res.size());
        assertEquals(List.of(date1, date2, date3, date4), res);
    }

    /** Upstream {@code shouldGenerateListSortWithAttribute}. */
    @Test
    void shouldGenerateListSortWithAttribute() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1) // single
                
                func FuncFoo:
                	inputs:
                		foos Foo (0..*)
                	output:
                		sortedFoos Foo (0..*)
                
                	set sortedFoos:
                		foos sort [item -> attr] // sort based on item attribute
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param foos\s
                	* @return sortedFoos\s
                	*/
                	public List<? extends Foo> evaluate(List<? extends Foo> foos) {
                		List<Foo.FooBuilder> sortedFoosBuilder = doEvaluate(foos);
                \t\t
                		final List<? extends Foo> sortedFoos;
                		if (sortedFoosBuilder == null) {
                			sortedFoos = null;
                		} else {
                			sortedFoos = sortedFoosBuilder.stream().map(Foo::build).collect(Collectors.toList());
                			objectValidator.validate(Foo.class, sortedFoos);
                		}
                \t\t
                		return sortedFoos;
                	}
                
                	protected abstract List<Foo.FooBuilder> doEvaluate(List<? extends Foo> foos);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Foo.FooBuilder> doEvaluate(List<? extends Foo> foos) {
                			if (foos == null) {
                				foos = Collections.emptyList();
                			}
                			List<Foo.FooBuilder> sortedFoos = new ArrayList<>();
                			return assignOutput(sortedFoos, foos);
                		}
                \t\t
                		protected List<Foo.FooBuilder> assignOutput(List<Foo.FooBuilder> sortedFoos, List<? extends Foo> foos) {
                			sortedFoos = toBuilder(MapperC.<Foo>of(foos)
                				.sort(item -> item.<String>map("getAttr", foo -> foo.getAttr())).getMulti());
                \t\t\t
                			return Optional.ofNullable(sortedFoos)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object foo4 = foo(classes, "d");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo4, foo2, foo3, foo1));
        assertEquals(4, res.size());
        assertEquals(List.of(foo1, foo2, foo3, foo4), res);
    }

    /** Upstream {@code shouldGenerateIntListReverse}. */
    @Test
    void shouldGenerateIntListReverse() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:\s
                	inputs:
                		numbers int (0..*)
                	output:
                		sortedNumbers int (0..*)
                
                	set sortedNumbers:
                		numbers
                			reverse // reverse (no sort)
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(4, 2, 3, 1));
        assertEquals(4, res.size());
        assertEquals(List.of(1, 3, 2, 4), res);
    }

    /** Upstream {@code shouldGenerateDateListSortThenReverse}. */
    @Test
    void shouldGenerateDateListSortThenReverse() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:\s
                	inputs:
                		dates date (0..*)
                	output:
                		sortedDates date (0..*)
                
                	set sortedDates:
                		dates\s
                			sort // sort items
                			reverse
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object date1 = date(classes, 2000, 1, 1);
        Object date2 = date(classes, 2000, 1, 2);
        Object date3 = date(classes, 2000, 2, 1);
        Object date4 = date(classes, 2001, 1, 1);
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(date4, date1, date2, date3));
        assertEquals(4, res.size());
        assertEquals(List.of(date4, date3, date2, date1), res);
    }

    /** Upstream {@code shouldGenerateListSortWithAttributeThenReverse}. */
    @Test
    void shouldGenerateListSortWithAttributeThenReverse() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1) // single
                
                func FuncFoo:
                	inputs:
                		foos Foo (0..*)
                	output:
                		sortedFoos Foo (0..*)
                
                	set sortedFoos:
                		foos\s
                			sort [item -> attr] // sort based on item attribute
                			reverse
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object foo4 = foo(classes, "d");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo4, foo2, foo3, foo1));
        assertEquals(4, res.size());
        assertEquals(List.of(foo4, foo3, foo2, foo1), res);
    }

    /** Upstream {@code shouldGenerateListReverseComplexType}. */
    @Test
    void shouldGenerateListReverseComplexType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1) // single
                
                func FuncFoo:
                	inputs:
                		foos Foo (0..*)
                	output:
                		sortedFoos Foo (0..*)
                
                	set sortedFoos:
                		foos\s
                			reverse
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "a");
        Object foo2 = foo(classes, "b");
        Object foo3 = foo(classes, "c");
        Object foo4 = foo(classes, "d");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo4, foo2, foo3, foo1));
        assertEquals(4, res.size());
        assertEquals(List.of(foo1, foo3, foo2, foo4), res);
    }

    // ---------------------------------------------------------------- helpers

    /** Upstream {@code createFoo(Map, String)}. */
    private static Object foo(Map<String, Class<?>> classes, String attr) {
        return UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("attr", attr), Map.of());
    }

    /** Upstream {@code createFoo(Map, boolean, String)}. */
    private static Object foo(Map<String, Class<?>> classes, boolean include, String attr) {
        return UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("include", include, "attr", attr), Map.of());
    }

    /** Upstream {@code createFoo2}. */
    private static Object foo2(Map<String, Class<?>> classes, boolean include,
            boolean include2, String attr) {
        return UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo2",
                Map.of("include", include, "include2", include2, "attr", attr), Map.of());
    }

    /** Upstream {@code createFooWithScheme}. */
    private static Object fooWithScheme(Map<String, Class<?>> classes, String attr, String scheme) {
        Object fieldWithMetaString = UpstreamPortHarness.createFieldWithMetaString(classes, attr, scheme);
        return UpstreamPortHarness.createInstanceUsingBuilder(classes, "FooWithScheme",
                Map.of("attr", fieldWithMetaString), Map.of());
    }

    /** Upstream {@code createBar}. */
    private static Object bar(Map<String, Class<?>> classes, List<Object> foos) {
        return UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar",
                Map.of(), Map.of("foos", foos));
    }

    /** Upstream {@code Date.of} — reflective on the isolated loader's released runtime. */
    private static Object date(Map<String, Class<?>> classes, int year, int month, int day) {
        try {
            Class<?> dateClass = UpstreamPortHarness.loadRuntimeClass(classes,
                    "com.rosetta.model.lib.records.Date");
            return dateClass.getMethod("of", int.class, int.class, int.class)
                    .invoke(null, year, month, day);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Date.of unavailable on the released runtime", e);
        }
    }
}
