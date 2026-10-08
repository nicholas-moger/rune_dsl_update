/*
 * Copyright 2024 REGnosys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rosetta.model.lib.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.common.base.CaseFormat;

/**
 * Locks for the U021 deferred path-lineage contract (census § 16e): deferral
 * until read (per derived field), materialize-once memoization, the eager-twin
 * equivalence battery over root/null/hop/list-index/deep shapes (expectations
 * recomputed in-test with the pre-deferral derivations — the identical guava
 * conversion, format and hash arithmetic), the cache-vs-guava witness battery,
 * benign-race convergence, the through-{@link Mapper.Path} observation pin,
 * and the builder single-shot independence corner the design documents. The
 * corpus-scale lock is the rune-equivalence O2 channel + the § 14f
 * text-capture instrument; these pins keep the lineage contract enforced
 * without the corpus.
 */
class MapperPathDeferredLineageTest {

	// --- the eager twins: the pre-deferral derivations, recomputed verbatim ---

	private static String eagerName(String getter) {
		if (getter.startsWith("get")) {
			return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, getter.substring(3));
		}
		return getter;
	}

	private static String eagerGetterAndContext(String getter, Optional<Integer> listIndex) {
		return listIndex
				.map(i -> String.format("%s[%s]", getter, i))
				.orElse(getter);
	}

	private static int eagerElementHash(String getter, Optional<Integer> listIndex) {
		final int prime = 31;
		int result = 1;
		result = prime * result + getter.hashCode();
		result = prime * result + listIndex.hashCode();
		result = prime * result + eagerName(getter).hashCode();
		return result;
	}

	private static int eagerPathHash(List<String> getters, List<Optional<Integer>> listIndexes) {
		int listHash = 1;
		for (int i = 0; i < getters.size(); i++) {
			listHash = 31 * listHash + eagerElementHash(getters.get(i), listIndexes.get(i));
		}
		return 31 * 1 + listHash;
	}

	// --- reflection probes (PathElement is private-nested by design) ---

	private static Object read(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}

	private static Object lastElement(MapperPath path) throws Exception {
		Field field = MapperPath.class.getDeclaredField("element");
		field.setAccessible(true);
		return field.get(path);
	}

	// --- the pins ---

	@Test
	void derivedFieldsDeferUntilTheirOwnRead() throws Exception {
		MapperPath path = MapperPath.builder().addFunctionName("getTradeDate");
		Object element = lastElement(path);
		assertNull(read(element, "name"), "name must not derive at construction");
		assertNull(read(element, "getterAndContext"), "getterAndContext must not derive at construction");
		assertNull(read(path, "pathElements"), "the flat element view must not materialize at construction");

		path.getFullPath();
		assertNotNull(read(element, "getterAndContext"), "getFullPath must materialize getterAndContext");
		assertNotNull(read(path, "pathElements"), "getFullPath must materialize the flat view");
		assertNull(read(element, "name"), "getFullPath must NOT force the name conversion");

		path.getNames();
		assertNotNull(read(element, "name"), "getNames must materialize the converted name");
	}

	@Test
	void memoizedFieldsMaterializeOnce() throws Exception {
		MapperPath path = MapperPath.builder().addRoot(String.class).toBuilder().addFunctionName("getTradeDate");
		path.getNames();
		path.getFullPath();
		Object element = lastElement(path);
		Object name = read(element, "name");
		Object getterAndContext = read(element, "getterAndContext");
		Object flat = read(path, "pathElements");
		path.getNames();
		path.getFullPath();
		path.hashCode();
		assertSame(name, read(element, "name"), "repeated reads must serve the memoized name instance");
		assertSame(getterAndContext, read(element, "getterAndContext"),
				"repeated reads must serve the memoized getterAndContext instance");
		assertSame(flat, read(path, "pathElements"), "repeated reads must serve the memoized flat view");
	}

	@Test
	void rootShapeMatchesTheEagerTwin() {
		MapperPath root = MapperPath.builder().addRoot(String.class);
		assertEquals(Arrays.asList(eagerName("String")), root.getNames());
		assertEquals(Arrays.asList("String"), root.getGetters());
		assertEquals("String", root.getLastName());
		assertEquals(eagerGetterAndContext("String", Optional.empty()), root.getFullPath());
		assertEquals(root.getFullPath(), root.toString());
		assertEquals(eagerPathHash(Arrays.asList("String"),
				Arrays.asList(Optional.empty())), root.hashCode());
	}

	@Test
	void nullShapeMatchesTheEagerTwin() {
		MapperPath nullPath = MapperPath.builder().addNull();
		assertEquals(Arrays.asList("Null"), nullPath.getNames());
		assertEquals("Null", nullPath.getFullPath());
		assertEquals(eagerPathHash(Arrays.asList("Null"),
				Arrays.asList(Optional.empty())), nullPath.hashCode());
	}

	@Test
	void hopAndListIndexShapesMatchTheEagerTwins() {
		MapperPath hop = MapperPath.builder().addRoot(String.class).toBuilder().addFunctionName("getTradeDate");
		assertEquals(Arrays.asList("String", eagerName("getTradeDate")), hop.getNames());
		assertEquals(Arrays.asList("String", "getTradeDate"), hop.getGetters());
		assertEquals(eagerName("getTradeDate"), hop.getLastName());
		assertEquals("String->getTradeDate", hop.getFullPath());

		MapperPath indexed = hop.toBuilder().addListFunctionName("getLegs", 2);
		assertEquals(eagerName("getLegs"), indexed.getLastName());
		assertEquals("String->getTradeDate->" + eagerGetterAndContext("getLegs", Optional.of(2)),
				indexed.getFullPath());
		assertEquals(eagerPathHash(
				Arrays.asList("String", "getTradeDate", "getLegs"),
				Arrays.asList(Optional.empty(), Optional.empty(), Optional.of(2))),
				indexed.hashCode());
	}

	@Test
	void deepChainMatchesTheEagerTwinAcrossEveryReadChannel() {
		MapperPath deep = MapperPath.builder().addRoot(String.class)
				.toBuilder().addFunctionName("getTradeLot")
				.toBuilder().addListFunctionName("getPriceQuantity", 0)
				.toBuilder().addFunctionName("Type coercion")
				.toBuilder().addFunctionName("getLeg2Payer");
		List<String> getters = Arrays.asList("String", "getTradeLot", "getPriceQuantity", "Type coercion", "getLeg2Payer");
		List<Optional<Integer>> indexes = Arrays.asList(
				Optional.empty(), Optional.empty(), Optional.of(0), Optional.empty(), Optional.empty());
		assertEquals(getters.stream().map(MapperPathDeferredLineageTest::eagerName).collect(Collectors.toList()),
				deep.getNames());
		assertEquals(getters, deep.getGetters());
		assertEquals(eagerName("getLeg2Payer"), deep.getLastName());
		assertEquals("String->getTradeLot->getPriceQuantity[0]->Type coercion->getLeg2Payer",
				deep.getFullPath());
		assertEquals(eagerPathHash(getters, indexes), deep.hashCode());
	}

	@Test
	void equalityAndOrderingMatchTheEagerSemantics() {
		MapperPath twinA = MapperPath.builder().addRoot(String.class).toBuilder().addListFunctionName("getLegs", 1);
		MapperPath twinB = MapperPath.builder().addRoot(String.class).toBuilder().addListFunctionName("getLegs", 1);
		assertEquals(twinA, twinB, "independently built identical chains must be equal");
		assertEquals(twinA.hashCode(), twinB.hashCode());
		assertEquals(0, twinA.compareTo(twinB));

		MapperPath alpha = MapperPath.builder().addFunctionName("getAlpha");
		MapperPath beta = MapperPath.builder().addFunctionName("getBeta");
		assertNotEquals(alpha, beta);
		assertTrue(alpha.compareTo(beta) < 0, "ordering must follow the CONVERTED names");
		assertTrue(beta.compareTo(alpha) > 0);
		// The distinguishing witness: "getA" converts to "a" (< "b") while the raw
		// getters order the other way ("b" < "getA") — this pair passes only under
		// converted-name comparison, so the pin cannot be satisfied by a
		// getter-ordering implementation.
		MapperPath convertedFirst = MapperPath.builder().addFunctionName("getA");
		MapperPath rawFirst = MapperPath.builder().addFunctionName("b");
		assertTrue(convertedFirst.compareTo(rawFirst) < 0,
				"ordering must follow the CONVERTED names even where the raw getters order oppositely");
		assertTrue(rawFirst.compareTo(convertedFirst) > 0);

		MapperPath indexed = MapperPath.builder().addListFunctionName("getLegs", 0);
		MapperPath unindexed = MapperPath.builder().addFunctionName("getLegs");
		assertNotEquals(indexed, unindexed);
		assertEquals(-1, indexed.compareTo(unindexed), "an indexed element orders before its unindexed twin");
		assertEquals(1, unindexed.compareTo(indexed));
		MapperPath indexedHigher = MapperPath.builder().addListFunctionName("getLegs", 3);
		assertTrue(indexed.compareTo(indexedHigher) < 0, "same-name elements order by index");

		MapperPath prefix = MapperPath.builder().addRoot(String.class);
		MapperPath longer = prefix.toBuilder().addFunctionName("getTradeDate");
		assertEquals(1, prefix.compareTo(longer), "the eager comparator returns 1 for the exhausted-left side");
		assertEquals(-1, longer.compareTo(prefix));
	}

	@Test
	void conversionCacheServesTheGuavaConversionByteForByte() throws Exception {
		List<String> getters = Arrays.asList(
				"getID", "getIDNumber", "getX509Certificate", "getLeg2Payer", "getTradeDate", "get");
		for (String getter : getters) {
			MapperPath path = MapperPath.builder().addFunctionName(getter);
			assertEquals(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, getter.substring(3)),
					path.getNames().get(0),
					"the cached conversion must be the guava conversion for " + getter);
		}
		for (String passThrough : Arrays.asList("Type coercion", "to-string", "Date", "Null", "String")) {
			MapperPath path = MapperPath.builder().addFunctionName(passThrough);
			assertEquals(passThrough, path.getNames().get(0),
					"non-get labels must pass through unconverted");
		}
		MapperPath first = MapperPath.builder().addFunctionName("getCachedOnce");
		MapperPath second = MapperPath.builder().addFunctionName("getCachedOnce");
		first.getNames();
		second.getNames();
		assertSame(read(lastElement(first), "name"), read(lastElement(second), "name"),
				"two elements over the same getter must share the cached conversion instance");
	}

	@Test
	void concurrentReadsConvergeOnTheSameRenderedLineage() throws Exception {
		MapperPath shared = MapperPath.builder().addRoot(String.class)
				.toBuilder().addFunctionName("getTradeLot")
				.toBuilder().addListFunctionName("getPriceQuantity", 4);
		String expectedFullPath = "String->getTradeLot->getPriceQuantity[4]";
		List<String> expectedNames = Arrays.asList("String", eagerName("getTradeLot"), eagerName("getPriceQuantity"));
		int threads = 8;
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		List<Throwable> failures = new CopyOnWriteArrayList<>();
		for (int i = 0; i < threads; i++) {
			Thread reader = new Thread(() -> {
				try {
					start.await();
					assertEquals(expectedFullPath, shared.getFullPath());
					assertEquals(expectedNames, shared.getNames());
					assertEquals(eagerPathHash(
							Arrays.asList("String", "getTradeLot", "getPriceQuantity"),
							Arrays.asList(Optional.empty(), Optional.empty(), Optional.of(4))),
							shared.hashCode());
				} catch (Throwable t) {
					failures.add(t);
				} finally {
					done.countDown();
				}
			});
			// Daemon so a pathologically-blocked reader can never keep the
			// test JVM alive past the await timeout below.
			reader.setDaemon(true);
			reader.start();
		}
		start.countDown();
		assertTrue(done.await(30, java.util.concurrent.TimeUnit.SECONDS), "concurrent readers must finish");
		assertTrue(failures.isEmpty(), () -> "concurrent readers must all see the eager rendering: " + failures);
		assertNotNull(read(shared, "pathElements"), "the flat view must have settled");
	}

	@Test
	void lineageObservedThroughThePublicMapperPathInterfaceMatchesTheEagerRendering() {
		Trade trade = new Trade("2026-08-11", Arrays.asList("leg0", "leg1"));
		Mapper<String> single = MapperS.of(trade).map("getTradeDate", Trade::getTradeDate);
		List<Mapper.Path> singlePaths = single.getPaths();
		assertEquals(1, singlePaths.size());
		Mapper.Path observed = singlePaths.get(0);
		assertEquals("Trade->getTradeDate", observed.toString());
		assertEquals("Trade->getTradeDate", observed.getFullPath());
		assertEquals(Arrays.asList("Trade", eagerName("getTradeDate")), observed.getNames());
		assertEquals(Arrays.asList("Trade", "getTradeDate"), observed.getGetters());
		assertEquals(eagerName("getTradeDate"), observed.getLastName());

		Mapper<String> listed = MapperS.of(trade).mapC("getLegs", Trade::getLegs);
		List<String> renderedPaths = listed.getPaths().stream()
				.map(Mapper.Path::toString)
				.collect(Collectors.toList());
		assertEquals(Arrays.asList("Trade->getLegs[0]", "Trade->getLegs[1]"), renderedPaths);
	}

	@Test
	void builderReuseYieldsIndependentSiblingPaths() {
		MapperPath base = MapperPath.builder().addRoot(String.class);
		MapperPath.MapperPathBuilder reused = base.toBuilder();
		MapperPath firstChild = reused.addFunctionName("getFirst");
		MapperPath secondChild = reused.addFunctionName("getSecond");
		assertEquals("String->getFirst", firstChild.getFullPath(),
				"a reused builder must not retro-mutate the first-built path");
		assertEquals("String->getSecond", secondChild.getFullPath());
		assertEquals("String", base.getFullPath(), "the base path must stay untouched");
	}

	private static final class Trade {
		private final String tradeDate;
		private final List<String> legs;

		private Trade(String tradeDate, List<String> legs) {
			this.tradeDate = tradeDate;
			this.legs = legs;
		}

		public String getTradeDate() {
			return tradeDate;
		}

		public List<String> getLegs() {
			return legs;
		}
	}
}
